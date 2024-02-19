package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.dto.rehabilitation.TestResultForm;
import smartfloor.domain.dto.rehabilitation.TestSurveyForm;
import smartfloor.domain.dto.rehabilitation.TestTrialForm;
import smartfloor.domain.dto.rehabilitation.WearableForm;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestSurveyType;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.domain.entities.rehabilitation.WearableWithSide;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.TestResultRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

class RehabilitationControllerIntegrationTest extends IntegrationTestBase {
    private static final String REHABILITATION_RESULTS_REST_ENDPOINT = "/rehabilitation/tests/results";
    @Autowired
    UserRepository userRepository;
    @Autowired
    TestResultRepository testResultRepository;
    @Autowired
    WearableRepository wearableRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    WearableGroupRepository wearableGroupRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testSoftDeleteTestResult() {
        // given
        User testUser = User.builder().tenant(getTestTenant()).authId("test_sd_tr_user").build();
        testUser = userRepository.save(testUser);
        TestResult testResultForSoftDel = createTestResult(testUser, TestType.TIMED_UP_N_GO, LocalDateTime.now(), null);
        TestResult anotherTestResult = createTestResult(testUser, TestType.TIMED_UP_N_GO, LocalDateTime.now(), null);

        // when
        HttpEntity<String> entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_RESULTS_REST_ENDPOINT + "/" + testResultForSoftDel.getId(),
                        getPort()
                ),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, testResultRepository.findByUserIdIn(List.of(testUser.getId())).size());
        assertEquals(1, testResultRepository
                .findByUserIdInAndDeleted(List.of(testUser.getId()), false).size());
    }

    @Test
    void testGetResultsAfterSoftDelete() throws JsonProcessingException {
        // given
        User testUser = User.builder().tenant(getTestTenant()).authId("test_gr_sd_tr_user").build();
        testUser = userRepository.save(testUser);
        Wearable w = Wearable.builder().id("wearable_get_tr_after_sd").build();
        w = wearableRepository.save(w);
        WearableGroup wg = WearableGroup.builder().wearables(List.of(w)).name("wg_get_tr_after_sd").build();
        wg = wearableGroupRepository.save(wg);
        Group group = Group.builder()
                .name("testSoftDelete")
                .managers(List.of(testUser))
                .tenant(getTestTenant())
                .users(List.of(testUser))
                .wearableGroup(wg)
                .build();
        groupRepository.save(group);
        TestResult testResultForSoftDel = createTestResult(testUser, TestType.TIMED_UP_N_GO, LocalDateTime.now(), w);

        // when
        HttpEntity<String> entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_RESULTS_REST_ENDPOINT + "/groups/" + group.getId(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<TestResult> testResults = mapper.readValue(response.getBody(), new TypeReference<List<TestResult>>() {
        });

        // then
        assertEquals(1, testResults.size());

        // then given
        testResultForSoftDel.setDeleted(true);
        testResultRepository.save(testResultForSoftDel);

        // then when
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_RESULTS_REST_ENDPOINT + "/groups/" + group.getId(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        testResults = mapper.readValue(response.getBody(), new TypeReference<List<TestResult>>() {
        });
        // then
        assertEquals(0, testResults.size());
    }

    @Test
    void testSoftDeleteForArchivedUser() {
        // given
        User testUser = User.builder().tenant(getTestTenant()).authId("test_sd_archived_user").build();
        testUser.archive();
        testUser = userRepository.save(testUser);
        TestResult testResultForSoftDel = createTestResult(testUser, TestType.TIMED_UP_N_GO, LocalDateTime.now(), null);
        // when
        HttpEntity<String> entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_RESULTS_REST_ENDPOINT + "/" + testResultForSoftDel.getId(),
                        getPort()
                ),
                HttpMethod.DELETE,
                entity,
                String.class
        );
        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testCreateTestResultForArchivedUser() throws JsonProcessingException {
        // given
        User archivedUser = User.builder().tenant(getTestTenant()).authId("test_create_tr_archived_user").build();
        archivedUser.archive();
        archivedUser = userRepository.save(archivedUser);
        TestResultForm form = TestResultForm.builder().build();
        form.setType(TestType.WALK);
        form.setBeginTime(LocalDateTime.now().minusMinutes(1));
        form.setEndTime(LocalDateTime.now());
        form.setUserId(archivedUser.getId());
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(REHABILITATION_RESULTS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testResultFieldsValidationOnCreateAndRetrieve() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).authId("test_create_retrieve_tr").build();
        user = userRepository.save(user);
        Wearable wearable = Wearable.builder().id("wearable_create_retrieve_tr").build();
        wearable = wearableRepository.save(wearable);
        WearableGroup wg = WearableGroup.builder().wearables(List.of(wearable)).name("wg_create_retrieve_tr").build();
        wg = wearableGroupRepository.save(wg);
        Group group = Group.builder()
                .name("group_create_retrieve_user")
                .tenant(getTestTenant())
                .wearableGroup(wg)
                .users(List.of(user))
                .build();
        groupRepository.save(group);
        LocalDateTime beginTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime endTime = LocalDateTime.now();

        TestTrialForm testTrialForm = TestTrialForm.builder()
                .beginTime(beginTime)
                .endTime(endTime)
                .build();

        Map<String, Object> surveyContent = new HashMap<>();
        surveyContent.put("score", 3);
        TestSurveyForm testSurveyForm = new TestSurveyForm(TestSurveyType.FAC, surveyContent);

        TestResultForm testResultForm = TestResultForm.builder()
                .userId(user.getId())
                .type(TestType.TEN_METER_WALKING)
                .beginTime(beginTime)
                .endTime(endTime)
                .trials(List.of(testTrialForm))
                .surveys(List.of(testSurveyForm))
                .wearable(new WearableForm(wearable.getId(), Wearable.Side.RIGHT))
                .build();

        // when
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(testResultForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(REHABILITATION_RESULTS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        TestResult testResult = mapper.readValue(response.getBody(), new TypeReference<TestResult>() {
        });

        // then
        // Check that the response entity contains the property "wearable"
        // (and not the old fields "wearableId" and "wearableSide").
        assertTrue(response.getBody().contains("wearable"));
        assertFalse(response.getBody().contains("wearableId"));
        assertFalse(response.getBody().contains("wearableSide"));
        // The creation endpoint should return a TR with trials, surveys, wearable information, etc.
        assertEquals(testResultForm.getUserId(), testResult.getUser().getId());
        assertEquals(testResultForm.getType(), testResult.getType());
        assertEquals(testResultForm.getBeginTime().truncatedTo(ChronoUnit.MILLIS), testResult.getBeginTime());
        assertEquals(testResultForm.getEndTime().truncatedTo(ChronoUnit.MILLIS), testResult.getEndTime());
        assertEquals(testResultForm.getTrials().size(), testResult.getTrials().size());
        assertEquals(testResultForm.getSurveys().size(), testResult.getSurveys().size());
        assertEquals(testResultForm.getWearable().getId(), testResult.getWearableWithSide().getWearable().getId());
        assertEquals(testResultForm.getWearable().getSide(), testResult.getWearableWithSide().getSide());

        // then when
        entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_RESULTS_REST_ENDPOINT + '/' + testResult.getId(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        testResult = mapper.readValue(response.getBody(), new TypeReference<TestResult>() {
        });
        assertTrue(response.getBody().contains("wearable"));
        assertFalse(response.getBody().contains("wearableId"));
        assertFalse(response.getBody().contains("wearableSide"));
        // then
        // The get endpoint should return the test result with all the same fields.
        assertEquals(testResultForm.getUserId(), testResult.getUser().getId());
        assertEquals(testResultForm.getType(), testResult.getType());
        assertEquals(testResultForm.getBeginTime().truncatedTo(ChronoUnit.MILLIS), testResult.getBeginTime());
        assertEquals(testResultForm.getEndTime().truncatedTo(ChronoUnit.MILLIS), testResult.getEndTime());
        assertEquals(testResultForm.getTrials().size(), testResult.getTrials().size());
        assertEquals(testResultForm.getSurveys().size(), testResult.getSurveys().size());
        assertEquals(testResultForm.getWearable().getId(), testResult.getWearableWithSide().getWearable().getId());
        assertEquals(testResultForm.getWearable().getSide(), testResult.getWearableWithSide().getSide());
    }

    private TestResult createTestResult(User user, TestType type, LocalDateTime beginAndEndTime, Wearable wearable) {
        TestTrialForm testTrialForm = TestTrialForm.builder()
                .beginTime(beginAndEndTime)
                .endTime(beginAndEndTime)
                .build();

        Map<String, Object> surveyContent = new HashMap<>();
        surveyContent.put("score", 3);
        TestSurveyForm testSurveyForm = new TestSurveyForm(TestSurveyType.FAC, surveyContent);
        TestResultForm testResultForm = TestResultForm.builder()
                .userId(user.getId())
                .type(type)
                .beginTime(beginAndEndTime)
                .endTime(beginAndEndTime)
                .trials(List.of(testTrialForm))
                .surveys(List.of(testSurveyForm))
                .build();

        if (wearable != null) {
            testResultForm.setWearable(new WearableForm(wearable.getId(), Wearable.Side.RIGHT));
        }

        TestResult tr = new TestResult(testResultForm);
        if (wearable != null) {
            tr.setWearableWithSide(new WearableWithSide(wearable, Wearable.Side.RIGHT));
        }
        tr.setUser(user);
        tr = testResultRepository.save(tr);
        return tr;
    }
}
