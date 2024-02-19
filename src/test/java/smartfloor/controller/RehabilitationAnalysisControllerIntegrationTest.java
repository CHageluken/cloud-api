package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.measure.MetricPrefix;
import javax.measure.UnitConverter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.dto.rehabilitation.TestResultForm;
import smartfloor.domain.dto.rehabilitation.TestSurveyForm;
import smartfloor.domain.dto.rehabilitation.TestTrialForm;
import smartfloor.domain.dto.rehabilitation.WearableForm;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Position;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestSurveyType;
import smartfloor.domain.entities.rehabilitation.TestTrial;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.domain.entities.rehabilitation.WearableWithSide;
import smartfloor.domain.indicators.fall.risk.FallRiskScore;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.indicators.rehabilitation.TargetDistance;
import smartfloor.domain.parameters.custom.AverageSpeed;
import smartfloor.domain.parameters.custom.WalkingTime;
import smartfloor.domain.tests.SixMinuteWalking;
import smartfloor.domain.tests.TenMeterWalking;
import smartfloor.domain.tests.TimedUpNGo;
import smartfloor.domain.tests.Walk;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.FloorRepository;
import smartfloor.repository.jpa.FootstepRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.TestResultRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;
import static tech.units.indriya.unit.Units.METRE;

class RehabilitationAnalysisControllerIntegrationTest extends IntegrationTestBase {
    private final String REHABILITATION_ANALYSIS_ENDPOINT = "/v1/analyses/rehabilitation";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    FloorRepository floorRepository;
    @Autowired
    FootstepRepository footstepRepository;
    @Autowired
    TestResultRepository testResultRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WearableRepository wearableRepository;
    @Autowired
    WearableGroupRepository wearableGroupRepository;
    @Autowired
    GroupRepository groupRepository;

    /**
     * TODO.
     */
    Wearable getTestWearable() {
        Wearable w = wearableRepository.findById("testWearableForRehabilitationAnalysis");
        if (w == null) {
            w = Wearable.builder()
                    .id("testWearableForRehabilitationAnalysis")
                    .build();
            w = wearableRepository.save(w);
        }
        return w;
    }

    /**
     * TODO.
     */
    WearableGroup getTestWearableGroup() {
        return wearableGroupRepository.findByName("testWearableGroupForRehabilitationAnalysis")
                .orElseGet(() -> wearableGroupRepository.save(
                        WearableGroup.builder()
                                .name("testWearableGroupForRehabilitationAnalysis")
                                .wearables(List.of(getTestWearable()))
                                .build()
                ));
    }

    /**
     * TODO.
     */
    Group getTestGroup() {
        return groupRepository.findByName("testGroupForRehabilitationAnalysis")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .name("testGroupForRehabilitationAnalysis")
                                .build())
                );
    }

    /**
     * TODO.
     */
    Group getTestGroup(List<User> users) {
        return groupRepository.findByName("testGroupForRehabilitationAnalysis")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .users(users)
                                .name("testGroupForRehabilitationAnalysis")
                                .build())
                );
    }

    /**
     * TODO.
     */
    @BeforeEach
    void setUp() {
        footstepRepository.deleteAll();
    }

    /**
     * TODO.
     */
    private List<Footstep> findFirstTenMeters(List<Footstep> footsteps) {
        List<Footstep> footstepsWithPosition = footsteps
                .stream()
                .filter(Footstep::hasPosition)
                .collect(Collectors.toUnmodifiableList());
        Double distance = 0.0;
        UnitConverter toMeters = MetricPrefix.MILLI(METRE).getConverterTo(METRE);
        for (int i = 0; i < footstepsWithPosition.size() - 1; i++) {
            distance += footstepsWithPosition.get(i)
                    .getPosition()
                    .distanceTo(footstepsWithPosition.get(i + 1).getPosition());
            final double distanceInMeters = toMeters.convert(distance).doubleValue();
            /* We find the sub list of footsteps that has at least 10 meters (the last footstep makes it 10 meters or a
            bit more). Sub list includes from 0 to i + 1 since we need to include the step beyond this iteration since
            that is what we take the last distance to (i+2 since the endpoint is exclusive). */
            if (distanceInMeters >= 10) return footstepsWithPosition.subList(0, i + 2);
        }
        return footstepsWithPosition;
    }

    @Test
    void testGetTenMeterWalkingTestResultsForUserWithinTimeWindow() throws IOException {
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("tmwt_test_user").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        // given
        List<smartfloor.domain.indicators.footstep.AverageSpeed> averageSpeeds = new ArrayList<>();
        Floor floor = Floor.builder().name("Test").build();
        floorRepository.save(floor);
        List<TestResult> testResults = buildRandomTestResults(TestType.TEN_METER_WALKING, user, wearable, 3);
        for (TestResult tr : testResults) {
            List<Footstep> footsteps =
                    buildRandomListOfFootsteps(floor, wearable, tr.getBeginTime(), tr.getEndTime(), 250);
            List<Footstep> firstTenMeters = findFirstTenMeters(footsteps);
            smartfloor.domain.indicators.footstep.AverageSpeed as =
                    smartfloor.domain.indicators.footstep.AverageSpeed.of(firstTenMeters);
            footstepRepository.saveAll(footsteps);
            averageSpeeds.add(as);
        }

        // when
        LocalDateTime begin = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_ANALYSIS_ENDPOINT +
                                "/tests/ten-meter-walking/users/" + user.getId() +
                                "?begin=" + begin.toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + end.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then
        List<TenMeterWalking> results =
                mapper.readValue(response.getBody(), new TypeReference<List<TenMeterWalking>>() {
                });
        assertEquals(testResults.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            assertEquals(averageSpeeds.get(i).getValue(), results.get(i).getIndicatorByName("AverageSpeed").getValue());
        }
    }

    @Test
    void testGetWalkTestResultsForUserWithinTimeWindow() throws IOException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("walk_test_user").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        List<CoveredDistance> coveredDistances = new ArrayList<>();
        List<AverageSpeed> averageSpeeds = new ArrayList<>();
        List<FallRiskScore> fallRiskScores = new ArrayList<>();
        Floor floor = Floor.builder().name("Test").build();
        floorRepository.save(floor);
        List<TestResult> testResults = buildRandomTestResults(TestType.WALK, user, wearable, 3);
        for (TestResult tr : testResults) {
            TimeWindow tw = new TimeWindow(tr.getBeginTime(), tr.getEndTime());
            List<Footstep> footsteps =
                    buildRandomListOfFootsteps(floor, wearable, tr.getBeginTime(), tr.getEndTime(), 25);
            CoveredDistance cd = CoveredDistance.of(footsteps);
            AverageSpeed as = AverageSpeed.withWalkingTime(footsteps, new WalkingTime(tw));
            FallRiskScore fallRiskScore = FallRiskScore.of(footsteps, tw.getBeginTime(), tw.getEndTime());
            footstepRepository.saveAll(footsteps);
            coveredDistances.add(cd);
            averageSpeeds.add(as);
            fallRiskScores.add(fallRiskScore);
        }

        // when
        final LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_ANALYSIS_ENDPOINT +
                                "/tests/walk/users/" + user.getId() +
                                "?begin=" + currentTime.minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + currentTime.plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET, entity, String.class
        );

        // then
        List<Walk> results = mapper.readValue(response.getBody(), new TypeReference<List<Walk>>() {
        });
        assertEquals(testResults.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            Walk current = results.get(i);
            assertEquals(coveredDistances.get(i).getValue(), current.getIndicatorByName("CoveredDistance").getValue());
            assertEquals(averageSpeeds.get(i).getValue(), current.getIndicatorByName("AverageSpeed").getValue());
            assertEquals(
                    fallRiskScores.get(i).getValue(),
                    current.getTrials().get(0).getIndicatorByName("FallRiskScore").getValue()
            );
        }
    }

    @Test
    void testGetTimedUpNGoTestResultsForUserWithinTimeWindow() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        userRepository.save(user);
        List<TestResult> testResults = buildRandomTestResults(TestType.TIMED_UP_N_GO, user, null, 3);
        List<WalkingTime> walkingTimes = new ArrayList<>();
        for (TestResult tr : testResults) {
            TimeWindow tw = new TimeWindow(tr.getBeginTime(), tr.getEndTime());
            WalkingTime wt = new WalkingTime(tw);
            walkingTimes.add(wt);
        }

        // when
        HttpEntity<String> entityTug = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_ANALYSIS_ENDPOINT +
                                "/tests/timed-up-and-go/users/" + user.getId() +
                                "?begin=" + LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET,
                entityTug,
                String.class
        );

        // then
        List<TimedUpNGo> results = mapper.readValue(response.getBody(), new TypeReference<List<TimedUpNGo>>() {
        });
        assertEquals(testResults.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            assertEquals(walkingTimes.get(i).getValue(), results.get(i).getIndicatorByName("WalkingTime").getValue());
        }
    }

    @Test
    void testGetSixMinuteWalkingTestResultsForUserWithinTimeWindow() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("six_test_user").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        List<CoveredDistance> coveredDistances = new ArrayList<>();
        List<smartfloor.domain.indicators.footstep.AverageSpeed> averageSpeeds = new ArrayList<>();
        Floor floor = Floor.builder().name("Test").build();
        floorRepository.save(floor);

        List<TestResult> testResults = buildRandomTestResults(TestType.SIX_MINUTE_WALKING, user, wearable, 3);
        for (TestResult tr : testResults) {
            List<Footstep> footsteps =
                    buildRandomListOfFootsteps(floor, wearable, tr.getBeginTime(), tr.getEndTime(), 25);
            CoveredDistance cd = CoveredDistance.of(footsteps);
            smartfloor.domain.indicators.footstep.AverageSpeed as =
                    smartfloor.domain.indicators.footstep.AverageSpeed.of(footsteps);
            footstepRepository.saveAll(footsteps);
            coveredDistances.add(cd);
            averageSpeeds.add(as);
        }

        // when
        final LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_ANALYSIS_ENDPOINT +
                                "/tests/six-minute-walking/users/" + user.getId() +
                                "?begin=" + currentTime.minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + currentTime.plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET, entity, String.class
        );
        List<SixMinuteWalking> results =
                mapper.readValue(response.getBody(), new TypeReference<List<SixMinuteWalking>>() {
                });
        // then
        assertEquals(testResults.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            assertEquals(
                    coveredDistances.get(i).getValue(),
                    results.get(i).getIndicatorByName("CoveredDistance").getValue()
            );
            assertEquals(averageSpeeds.get(i).getValue(), results.get(i).getIndicatorByName("AverageSpeed").getValue());
        }
    }

    @Test
    void testGetTestResultsForTenant() throws JsonProcessingException {
        // given
        long secondTenantId = 2L;
        AccessScopeContext.INSTANCE.setTenantId(secondTenantId); // so that we are allowed to create for tenant 2
        Tenant tenant =
                tenantRepository.findById(secondTenantId).get(); // assumption is that it was created by migration
        User user = User.builder().tenant(tenant).build();
        user = userRepository.save(user);
        TestResult testResult = TestResult.builder()
                .user(user)
                .type(TestType.TEN_METER_WALKING)
                .beginTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now())
                .build();
        testResultRepository.save(testResult);
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());

        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/rehabilitation/tests/results", getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then
        List<TestResult> testResults = mapper.readValue(response.getBody(), new TypeReference<List<TestResult>>() {
        });

        boolean testResultBelongsToCurrentTenant = testResults.stream()
                .filter(tr -> tr.getId() == testResult.getId())
                .toList().size() > 0;

        assertFalse(testResultBelongsToCurrentTenant);
    }

    /**
     * <p>Tests that the correct UserInfo is used for the calculation of TargetDistance.</p>
     * The test records a 6mwt for a user with no UserInfo (and then performs the first TargetDistance check),
     * updates the user's info a few times, then creates another 6mwt result, which is expected to calculate
     * TargetDistance using the user's current info (second check).
     * Finally, we update the user info once more and test if the second TR still keeps its previous TargetDistance
     * (third check).
     */
    @Test
    void testGet6MWTTargetDistance() throws JsonProcessingException {
        // given: User with no UserInfo
        Tenant tenant = getTestTenant();
        Group group = getTestGroup();
        Wearable wearable = getTestWearable();

        User testUser = User.builder().tenant(getTestTenant()).authId("test_6mwt_user").build();
        testUser = userRepository.save(testUser);
        TestResult tr1 = createTestResultWithTimestamps(
                testUser,
                TestType.SIX_MINUTE_WALKING,
                LocalDateTime.now().minusMinutes(2),
                wearable
        );
        Floor floor = Floor.builder().name("Test").build();
        floor = floorRepository.save(floor);
        List<Footstep> footsteps = buildRandomListOfFootsteps(
                floor,
                tr1.getWearableWithSide().getWearable(),
                tr1.getBeginTime(),
                tr1.getEndTime(),
                3
        );
        footstepRepository.saveAll(footsteps);
        // when
        HttpEntity<String> entity
                = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response
                = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_ANALYSIS_ENDPOINT + "/tests/six-minute-walking/users/" + testUser.getId() +
                                "?begin=" +
                                tr1.getBeginTime().minusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + tr1.getEndTime().plusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<SixMinuteWalking> results =
                mapper.readValue(response.getBody(), new TypeReference<List<SixMinuteWalking>>() {
                });
        // then: Expect the only test result (TR) to have a TargetDistance (TD) with a value of 0,
        // since the user is created with no UserInfo.
        assertEquals(0.0, results.get(0).getIndicatorByName("TargetDistance").getValue());

        // then given: Update the UserInfo twice and create another TR
        UserInfo userInfo = UserInfo.builder()
                .age(80)
                .weight(70)
                .height(165)
                .gender("f")
                .build();
        testUser.setInfo(userInfo);
        userRepository.save(testUser);
        userInfo = UserInfo.builder()
                .age(81)
                .weight(70)
                .height(165)
                .gender("f")
                .build();
        testUser.setInfo(userInfo);
        userRepository.save(testUser);
        TestResult tr2 = createTestResultWithTimestamps(
                testUser,
                TestType.SIX_MINUTE_WALKING,
                LocalDateTime.now(),
                wearable
        );
        footsteps = buildRandomListOfFootsteps(
                floor,
                tr2.getWearableWithSide().getWearable(),
                tr2.getBeginTime(),
                tr2.getEndTime(),
                3
        );
        footstepRepository.saveAll(footsteps);
        // then when
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_ANALYSIS_ENDPOINT + "/tests/six-minute-walking/users/" + testUser.getId() +
                                "?begin=" +
                                tr2.getBeginTime().minusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + tr2.getEndTime().plusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        results = mapper.readValue(response.getBody(), new TypeReference<List<SixMinuteWalking>>() {
        });
        TargetDistance expected = new TargetDistance(userInfo);
        // then: The second TR should have a TD that uses the current UserInfo for calculations
        assertEquals(expected.getValue(), results.get(0).getIndicatorByName("TargetDistance").getValue());

        // then given: We update the user info once more
        userInfo = UserInfo.builder()
                .age(85)
                .weight(76)
                .height(164)
                .gender("f")
                .build();
        testUser.setInfo(userInfo);
        userRepository.save(testUser);
        // then when: We fetch the second TR's TD again
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        REHABILITATION_ANALYSIS_ENDPOINT + "/tests/six-minute-walking/users/" + testUser.getId() +
                                "?begin=" +
                                tr2.getBeginTime().minusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + tr2.getEndTime().plusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        results = mapper.readValue(response.getBody(), new TypeReference<List<SixMinuteWalking>>() {
        });
        // then: The TD should be the same as in the previous check (because the UserInfoHistory is used at this point)
        assertEquals(expected.getValue(), results.get(0).getIndicatorByName("TargetDistance").getValue());
    }

    /**
     * TODO.
     */
    private TestResult createTestResultWithTimestamps(
            User user,
            TestType type,
            LocalDateTime beginAndEndTime,
            Wearable wearable
    ) {
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
                .endTime(beginAndEndTime.plusMinutes(1))
                .trials(List.of(testTrialForm))
                .surveys(List.of(testSurveyForm))
                .wearable(new WearableForm(wearable.getId(), Wearable.Side.RIGHT))
                .build();

        TestResult tr = new TestResult(testResultForm);
        tr.setWearableWithSide(new WearableWithSide(wearable, Wearable.Side.RIGHT));
        tr.setUser(user);
        TestTrial t = new TestTrial(testTrialForm);
        tr.setTrials(List.of(t));
        tr = testResultRepository.save(tr);
        return tr;
    }

    /**
     * Utility method to build a random list of footsteps for use in tests.
     */
    private List<Footstep> buildRandomListOfFootsteps(
            Floor floor,
            Wearable wearable,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            int amount
    ) {
        List<Footstep> randomFootsteps = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Footstep randomFootstep = new Footstep();
            randomFootstep.setFloor(floor);
            randomFootstep.setWearable(wearable);
            Position randomPosition = new Position(
                    ThreadLocalRandom.current().nextInt(amount * 2),
                    ThreadLocalRandom.current().nextInt(amount * 2)
            );
            randomFootstep.setTime(fromTime.plusSeconds(i));
            if (randomFootstep.getTime().isAfter(toTime)) {
                throw new IllegalArgumentException("Amount of footsteps provided exceeds test result time range.");
            }

            randomFootstep.setPosition(randomPosition);
            randomFootsteps.add(randomFootstep);
        }
        return randomFootsteps;
    }

    /**
     * TODO.
     */
    private List<TestResult> buildRandomTestResults(TestType type, User user, Wearable wearable, int amount) {
        List<TestResult> testResults = new ArrayList<>();
        for (int i = amount; i > 0; i--) {
            LocalDateTime beginTime = LocalDateTime.now().minusHours(i).minusMinutes(40);
            LocalDateTime endTime = LocalDateTime.now().minusHours(i).minusMinutes(20);
            TestTrialForm testTrialForm = TestTrialForm.builder()
                    .beginTime(beginTime)
                    .endTime(endTime)
                    .build();

            TestResultForm testResultForm = TestResultForm.builder()
                    .userId(user.getId())
                    .type(type)
                    .beginTime(beginTime)
                    .endTime(endTime)
                    .trials(List.of(testTrialForm))
                    .build();
            TestResult tr = new TestResult(testResultForm);
            if (wearable != null) {
                tr.setWearableWithSide(new WearableWithSide(wearable, Wearable.Side.RIGHT));
            }
            tr.setUser(user);
            TestTrial tt = new TestTrial(testTrialForm);
            tr.setTrials(List.of(tt));
            tr = testResultRepository.save(tr);
            testResults.add(tr);
        }
        return testResults;
    }
}
