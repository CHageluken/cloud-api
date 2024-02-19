package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestSurvey;
import smartfloor.domain.entities.rehabilitation.TestSurveyType;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.util.TestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdditionalInfoMigrationIntegrationTest extends IntegrationTestBase {
    private static final String REHABILITATION_RESULTS_REST_ENDPOINT = "/rehabilitation/tests/results";
    private final ObjectMapper mapper = new ObjectMapper();
    private List<TestResult> testResults = new ArrayList<>();

    /**
     * TODO.
     */
    @BeforeEach
    void setUp() throws JsonProcessingException {
        HttpEntity<String> entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(REHABILITATION_RESULTS_REST_ENDPOINT, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        testResults = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
    }

    /**
     * <p>Tests if data extraction from the additionalInfo field is successful.</p>
     * <p>Since the test makes use of a test migration (V44.1) followed by the extraction migration (V45),
     * we want to make sure that the test results are persisted. We have therefore added Flyway as a dependency
     * to this test.</p>
     * Note: It would be nice if instead of one test we had one per rehabilitation test type, but we want to avoid
     * calling flyway.migrate() multiple times.
     */
    @Test
    void testEvaluateFieldsPerTestResultWalk() {
        // WALK
        TestResult walk = testResults.stream()
                .filter(tr -> tr.getType() == TestType.WALK).findFirst().get();
        // Check trials. A trial must have been created
        assertEquals(1, walk.getTrials().size());
        assertEquals(walk.getBeginTime(), walk.getTrials().get(0).getBeginTime());
        assertEquals(walk.getEndTime(), walk.getTrials().get(0).getEndTime());
        // Check wearable & side.
        // The field "defaultDevice" (h_37) has higher priority than "optionalDevice" (h_50).
        assertEquals("heelable_37", walk.getWearableWithSide().getWearable().getId());
        assertEquals(Wearable.Side.LEFT, walk.getWearableWithSide().getSide());
        assertNotEquals("heelable_50", walk.getWearableWithSide().getWearable().getId());
        // Check surveys. No surveys should exist for this test type.
        assertEquals(0, walk.getSurveys().size());
    }

    @Test
    void testEvaluateFieldsPerTestResultTug() {
        // TUG
        TestResult tug = testResults.stream()
                .filter(tr -> tr.getType() == TestType.TIMED_UP_N_GO).findFirst().get();
        // Check trials
        assertEquals(2, tug.getTrials().size());
        assertEquals(tug.getBeginTime(), tug.getTrials().get(0).getBeginTime());
        assertEquals(tug.getEndTime(), tug.getTrials().get(1).getEndTime());
        // Check wearable & side. Must NOT be present.
        assertNull(tug.getWearableWithSide());
        // Check surveys.
        // No surveys should exist in this case, "additionalInfo" of this TR does not contain a "surveys" field.
        assertEquals(0, tug.getSurveys().size());
    }

    @Test
    void testEvaluateFieldsPerTestResult10MWT() {
        // 10MWT
        TestResult tmwt = testResults.stream()
                .filter(tr -> tr.getType() == TestType.TEN_METER_WALKING).findFirst().get();
        // Check trials
        assertEquals(2, tmwt.getTrials().size());
        assertEquals(tmwt.getBeginTime(), tmwt.getTrials().get(0).getBeginTime());
        assertEquals(tmwt.getEndTime(), tmwt.getTrials().get(1).getEndTime());
        // Check wearable & side
        assertEquals("EFS33rdFF20", tmwt.getWearableWithSide().getWearable().getId());
        assertEquals(Wearable.Side.LEFT, tmwt.getWearableWithSide().getSide());
        // Check surveys. All surveys must be present.
        List<TestSurvey> tmwtSurveys = tmwt.getSurveys();
        assertEquals(5, tmwtSurveys.size());
        TestSurvey tmwtFesI = tmwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.FES_I).findFirst().get();
        assertEquals(30, tmwtFesI.getContent().get("totalScore"));
        TestSurvey tmwtBORG =
                tmwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.BORG_RPE).findFirst().get();
        assertEquals(6, tmwtBORG.getContent().get("score"));
        TestSurvey tmwtFAC = tmwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.FAC).findFirst().get();
        assertEquals(5, tmwtFAC.getContent().get("score"));
        TestSurvey tmwtNRS = tmwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.NRS).findFirst().get();
        assertEquals(7, tmwtNRS.getContent().get("score"));
        TestSurvey tmwtNPRS = tmwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.NPRS).findFirst().get();
        assertEquals(10, tmwtNPRS.getContent().get("score"));
    }

    @Test
    void testEvaluateFieldsPerTestResult6MWT() {
        // 6MWT
        TestResult smwt = testResults.stream()
                .filter(tr -> tr.getType() == TestType.SIX_MINUTE_WALKING).findFirst().get();
        // Check trials
        assertEquals(2, smwt.getTrials().size());
        assertEquals(smwt.getBeginTime(), smwt.getTrials().get(0).getBeginTime());
        assertEquals(smwt.getEndTime(), smwt.getTrials().get(1).getEndTime());
        // Check wearable & side
        assertEquals("heelable_48", smwt.getWearableWithSide().getWearable().getId());
        assertEquals(Wearable.Side.RIGHT, smwt.getWearableWithSide().getSide());
        // Check surveys. All surveys must be present.
        List<TestSurvey> smwtSurveys = smwt.getSurveys();
        assertEquals(3, smwtSurveys.size());
        TestSurvey smwtFesI = smwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.FES_I).findFirst().get();
        assertEquals(0, smwtFesI.getContent().get("totalScore"));
        TestSurvey smwtFAC = smwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.FAC).findFirst().get();
        assertEquals(4, smwtFAC.getContent().get("score"));
        TestSurvey smwtNPRS = smwtSurveys.stream().filter(s -> s.getType() == TestSurveyType.NPRS).findFirst().get();
        assertEquals(-1, smwtNPRS.getContent().get("score"));
    }
}
