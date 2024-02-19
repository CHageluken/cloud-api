package smartfloor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.measure.MetricPrefix;
import javax.measure.UnitConverter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.dto.rehabilitation.TestTrialForm;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Position;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestTrial;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.domain.entities.rehabilitation.WearableWithSide;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.indicators.fall.risk.FallRiskScore;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.indicators.footstep.WalkingTime;
import smartfloor.domain.indicators.rehabilitation.TargetDistance;
import smartfloor.domain.tests.SixMinuteWalking;
import smartfloor.domain.tests.TenMeterWalking;
import smartfloor.domain.tests.TimedUpNGo;
import smartfloor.domain.tests.Walk;
import smartfloor.repository.jpa.TestResultRepository;
import smartfloor.repository.jpa.UserInfoHistoryRepository;
import static tech.units.indriya.unit.Units.METRE;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class RehabilitationAnalysisServiceTest {
    @Mock
    private FootstepService footstepService;
    @Mock
    private TestResultRepository testResultRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private UserInfoHistoryRepository userInfoHistoryRepository;
    @InjectMocks
    private RehabilitationAnalysisService rehabilitationAnalysisService;

    /**
     * Test case that computes a list of ten meter walking tests from a given list of test results.
     */
    @Test
    void testComputeTenMeterWalkingTestForWearableWithinTimeWindow() {
        // given: a random list of 250 footsteps that we assert to be more than 10 meters long
        Wearable w = Wearable.builder().id("Test").build();
        List<Footstep> footsteps = buildRandomListOfFootsteps(250, w);
        CoveredDistance cd = CoveredDistance.of(footsteps);
        UnitConverter toMeters = MetricPrefix.MILLI(METRE).getConverterTo(METRE);
        boolean randomListHasMoreThanTenMeters = toMeters.convert(cd.getValue()).intValue() > 10;
        // make sure we start with a list of footsteps that's more than 10 meters
        assertTrue(randomListHasMoreThanTenMeters);

        // given: a sub list of this list of footsteps of which the last makes it reach 10 meters or a bit more
        int indexOfFootstepFirstTenMeters = findIndexOfFirstTenMetersOfFootsteps(footsteps);
        List<Footstep> firstTenMeters = footsteps.subList(0, indexOfFootstepFirstTenMeters);
        TimeWindow tw = new TimeWindow(
                firstTenMeters.get(0).getTime(),
                firstTenMeters.get(firstTenMeters.size() - 1).getTime()
        );
        AverageSpeed as =
                AverageSpeed.withWalkingTime(firstTenMeters, WalkingTime.of(tw.getBeginTime(), tw.getEndTime()));
        double expectedSpeed = as.getValue();

        // when
        Mockito.when(footstepService.getForWearableWithinTimeWindow(w, tw))
                .thenReturn(firstTenMeters);
        TenMeterWalking tmw =
                rehabilitationAnalysisService.computeTenMeterWalkingTestForWearableWithinTimeWindow(w, tw);

        // then
        assertEquals(expectedSpeed, tmw.getIndicators().get(0).getValue());
    }

    /**
     * Test case that computes a ten meter walking test from a given set of random footsteps and a time window.
     * The time window is used to define the duration of the test and the walking time. This is an alternative to
     * determining the walking time from the footsteps itself (time between first and last footstep).
     */
    @Test
    void testComputeTenMeterWalkingTestResults() {
        Wearable w = Wearable.builder().id("Test").build();
        // given: a random list of 250 footsteps that we assert to be more than 10 meters long
        List<Footstep> footsteps = buildRandomListOfFootsteps(250, w);
        CoveredDistance cd = CoveredDistance.of(footsteps);
        UnitConverter toMeters = MetricPrefix.MILLI(METRE).getConverterTo(METRE);
        boolean randomListHasMoreThanTenMeters = toMeters.convert(cd.getValue()).intValue() > 10;
        // make sure we start with a list of footsteps that's more than 10 meters
        assertTrue(randomListHasMoreThanTenMeters);

        // given: a sub list of this list of footsteps of which the last makes it reach 10 meters or a bit more
        int indexOfFootstepFirstTenMeters = findIndexOfFirstTenMetersOfFootsteps(footsteps);
        List<Footstep> firstTenMeters = footsteps.subList(0, indexOfFootstepFirstTenMeters);
        TimeWindow tw = new TimeWindow(
                firstTenMeters.get(0).getTime(),
                firstTenMeters.get(firstTenMeters.size() - 1).getTime()
        );
        AverageSpeed as =
                AverageSpeed.withWalkingTime(firstTenMeters, WalkingTime.of(tw.getBeginTime(), tw.getEndTime()));
        double speedOfTenMetersOfFootsteps = as.getValue();

        // when
        LocalDateTime beginTime = footsteps.get(0).getTime();
        LocalDateTime endTime = footsteps.get(footsteps.size() - 1).getTime();
        TestTrialForm testTrialForm = TestTrialForm.builder()
                .beginTime(beginTime)
                .endTime(endTime)
                .build();
        TestTrial trial = new TestTrial(testTrialForm);
        TestResult tr = TestResult.builder()
                .type(TestType.TEN_METER_WALKING)
                .beginTime(beginTime)
                .endTime(endTime)
                .trials(List.of(trial))
                .wearableWithSide(new WearableWithSide(w, Wearable.Side.RIGHT))
                .build();

        Mockito.when(footstepService.getForWearableWithinTimeWindow(Mockito.any(Wearable.class), Mockito.any()))
                .thenReturn(firstTenMeters);
        List<TenMeterWalking> tmwtList = rehabilitationAnalysisService.computeTenMeterWalkingTestResults(List.of(tr));
        TenMeterWalking tmwt = tmwtList.get(0);

        // then
        Map<String, Indicator> byName = tmwt.getTrials().get(0).getIndicators()
                .stream().collect(Collectors.toMap(Indicator::getName, Function.identity()));

        smartfloor.domain.indicators.footstep.AverageSpeed tmwtAs =
                (smartfloor.domain.indicators.footstep.AverageSpeed) byName.get("AverageSpeed");
        double speedOfTenMeterWalkingTest = tmwtAs.getValue();

        /* The average speed should be equal to the average speed that we obtained from our list of footsteps that first
         reach 10 meters (or a bit more). */
        assertEquals(speedOfTenMetersOfFootsteps, speedOfTenMeterWalkingTest, 0.0);
    }

    /**
     * Test case that computes a list of timed up n go tests from a give list of test results.
     */
    @Test
    void testComputeTimedUpNGoTestResults() {
        LocalDateTime now = LocalDateTime.now();
        TimeWindow tw = new TimeWindow(now.minusMinutes(5), now);
        WalkingTime wt = WalkingTime.of(tw.getBeginTime(), tw.getEndTime());

        // when
        LocalDateTime beginTime = tw.getBeginTime();
        LocalDateTime endTime = tw.getEndTime();
        TestTrialForm testTrialForm = TestTrialForm.builder()
                .beginTime(beginTime)
                .endTime(endTime)
                .build();
        TestTrial trial = new TestTrial(testTrialForm);
        TestResult tr = TestResult.builder()
                .type(TestType.TIMED_UP_N_GO)
                .beginTime(beginTime)
                .endTime(endTime)
                .trials(List.of(trial))
                .build();
        List<TimedUpNGo> tungList = rehabilitationAnalysisService.computeTimedUpNGoTestResults(List.of(tr));
        TimedUpNGo tung = tungList.get(0);

        // then
        Map<String, Indicator> byName = tung.getIndicators().stream()
                .collect(Collectors.toMap(Indicator::getName, Function.identity()));
        smartfloor.domain.indicators.footstep.WalkingTime tungWt =
                (smartfloor.domain.indicators.footstep.WalkingTime) byName.get("WalkingTime");
        double tungWtValue = tungWt.getValue();
        assertEquals(wt.getValue(), tungWtValue, 0.0);
    }

    /**
     * Test case that computes a six minute walking test from a given set of random footsteps and a time window.
     * The time window is used to define the duration of the test and the walking time. This is an alternative to
     * determining the walking time from the footsteps itself (time between first and last footstep).
     */
    @Test
    void testComputeSixMinuteWalkingTestForWearableWithinTimeWindow() {
        Wearable w = Wearable.builder().id("Test").build();
        List<Footstep> footsteps = buildRandomListOfFootsteps(100, w);
        CoveredDistance cd = CoveredDistance.of(footsteps);
        AverageSpeed as = AverageSpeed.of(footsteps);
        double cdValue = cd.getValue();
        double asValue = as.getValue();

        // when
        Mockito.when(footstepService.getForWearableWithinTimeWindow(
                        Mockito.any(Wearable.class),
                        Mockito.any(TimeWindow.class)
                ))
                .thenReturn(footsteps);
        SixMinuteWalking smwt = rehabilitationAnalysisService
                .computeSixMinuteWalkingTestForWearableWithinTimeWindow(w, new TimeWindow());

        // then
        Map<String, Indicator> indicatorsGroupedByName = smwt.getIndicators().stream()
                .collect(Collectors.toMap(Indicator::getName, Function.identity()));
        CoveredDistance smwtCd = (CoveredDistance) indicatorsGroupedByName.get("CoveredDistance");
        AverageSpeed smwtAs = (AverageSpeed) indicatorsGroupedByName.get("AverageSpeed");
        TargetDistance targetDistance = (TargetDistance) indicatorsGroupedByName.get("TargetDistance");
        double cdValueOfTest = smwtCd.getValue();
        double asValueOfTest = smwtAs.getValue();
        assertEquals(cdValue, cdValueOfTest, 0.0);
        assertEquals(asValue, asValueOfTest, 0.0);
        // Since we did not pass any user info, the value of this indicator should be 0
        assertEquals(0.0, targetDistance.getValue());
    }

    /**
     * Test case that computes a list of six minute walking tests from a list of test results.
     */
    @Test
    void testComputeSixMinuteWalkingTestResults() {
        UserInfo ui = UserInfo.builder()
                .gender("m")
                .height(160)
                .weight(70)
                .age(80)
                .build();
        User u = User.builder().build();
        u.setInfo(ui);
        Wearable w = Wearable.builder().id("Test").build();
        List<Footstep> footsteps = buildRandomListOfFootsteps(100, w);
        CoveredDistance cd = CoveredDistance.of(footsteps);
        AverageSpeed as = AverageSpeed.of(footsteps);
        TargetDistance td = new TargetDistance(ui);
        double cdValue = cd.getValue();
        double asValue = as.getValue();
        double tdValue = td.getValue();
        LocalDateTime beginTime = footsteps.get(0).getTime();
        LocalDateTime endTime = footsteps.get(footsteps.size() - 1).getTime();
        TestTrialForm testTrialForm = TestTrialForm.builder()
                .beginTime(beginTime)
                .endTime(endTime)
                .build();
        TestTrial trial = new TestTrial(testTrialForm);
        TestResult tr = TestResult.builder()
                .type(TestType.SIX_MINUTE_WALKING)
                .beginTime(beginTime)
                .endTime(endTime)
                .trials(List.of(trial))
                .wearableWithSide(new WearableWithSide(w, Wearable.Side.RIGHT))
                .user(u)
                .build();
        // when
        Mockito.when(footstepService.getForWearableWithinTimeWindow(
                        Mockito.any(Wearable.class),
                        Mockito.any(TimeWindow.class)
                ))
                .thenReturn(footsteps);
        List<SixMinuteWalking> smwtList = rehabilitationAnalysisService.computeSixMinuteWalkingTestResults(List.of(tr));
        SixMinuteWalking smwt = smwtList.get(0);

        // then
        Map<String, Indicator> indicatorsGroupedByName = smwt.getIndicators().stream()
                .collect(Collectors.toMap(Indicator::getName, Function.identity()));
        CoveredDistance smwtCd = (CoveredDistance) indicatorsGroupedByName.get("CoveredDistance");
        AverageSpeed smwtAs = (AverageSpeed) indicatorsGroupedByName.get("AverageSpeed");
        TargetDistance smwtTd = (TargetDistance) indicatorsGroupedByName.get("TargetDistance");
        double cdValueOfTest = smwtCd.getValue();
        double asValueOfTest = smwtAs.getValue();
        double tdValueOfTest = smwtTd.getValue();
        assertEquals(cdValue, cdValueOfTest, 0.0);
        assertEquals(asValue, asValueOfTest, 0.0);
        assertEquals(tdValue, tdValueOfTest, 0.0);
    }

    /**
     * Test case that computes a walk test from a given set of random footsteps and a time window.
     * The time window is used to define the duration of the test and the walking time. This is an alternative to
     * determining the walking time from the footsteps itself (time between first and last footstep).
     */
    @Test
    void testComputeWalkTestForWearableWithinTimeWindow() {
        Wearable w = Wearable.builder().id("Test").build();
        List<Footstep> footsteps = buildRandomListOfFootsteps(100, w);
        TimeWindow tw = new TimeWindow(LocalDateTime.now().minusMinutes(5), LocalDateTime.now());
        CoveredDistance cd = CoveredDistance.of(footsteps);
        AverageSpeed as = AverageSpeed.withWalkingTime(
                footsteps,
                WalkingTime.of(tw.getBeginTime(), tw.getEndTime())
        );
        FallRiskScore fallRiskScore = FallRiskScore.of(footsteps, tw.getBeginTime(), tw.getEndTime());
        double cdValue = cd.getValue();
        double asValue = as.getValue();

        // when
        Mockito.when(footstepService.getForWearableWithinTimeWindow(
                        Mockito.any(Wearable.class),
                        Mockito.any(TimeWindow.class)
                ))
                .thenReturn(footsteps);
        Walk walk = rehabilitationAnalysisService.computeWalkTestForWearableWithinTimeWindow(w, tw);

        // then
        Map<String, Indicator> indicatorsGroupedByName = walk.getIndicators().stream()
                .collect(Collectors.toMap(Indicator::getName, Function.identity()));
        Map<String, Indicator> trialIndicatorsGroupedByName = walk.getTrials().get(0)
                .getIndicators().stream().collect(Collectors.toMap(Indicator::getName, Function.identity()));
        CoveredDistance walkCd = (CoveredDistance) indicatorsGroupedByName.get("CoveredDistance");
        AverageSpeed walkAs = (AverageSpeed) indicatorsGroupedByName.get("AverageSpeed");
        FallRiskScore frsaOfTest = (FallRiskScore) trialIndicatorsGroupedByName.get("FallRiskScore");
        double cdValueOfTest = walkCd.getValue();
        double asValueOfTest = walkAs.getValue();
        assertEquals(cdValue, cdValueOfTest, 0.0);
        assertEquals(asValue, asValueOfTest, 0.0);
        assertEquals(fallRiskScore.getValue(), frsaOfTest.getValue());
    }

    /**
     * Test case that computes a list of walk tests from a list of test results.
     */
    @Test
    void testComputeWalkTestResults() {
        Wearable w = Wearable.builder().id("Test").build();
        List<Footstep> footsteps = buildRandomListOfFootsteps(100, w);
        TimeWindow tw = new TimeWindow(LocalDateTime.now().minusMinutes(5), LocalDateTime.now());
        CoveredDistance cd = CoveredDistance.of(footsteps);
        AverageSpeed as = AverageSpeed.of(footsteps);
        FallRiskScore fallRiskScore = FallRiskScore.of(footsteps, tw.getBeginTime(), tw.getEndTime());
        double cdValue = cd.getValue();
        double asValue = as.getValue();

        // when
        LocalDateTime beginTime = footsteps.get(0).getTime();
        LocalDateTime endTime = footsteps.get(footsteps.size() - 1).getTime();
        TestTrialForm testTrialForm = TestTrialForm.builder()
                .beginTime(beginTime)
                .endTime(endTime)
                .build();
        TestTrial trial = new TestTrial(testTrialForm);
        TestResult tr = TestResult.builder()
                .type(TestType.WALK)
                .beginTime(beginTime)
                .endTime(endTime)
                .trials(List.of(trial))
                .wearableWithSide(new WearableWithSide(w, Wearable.Side.RIGHT))
                .build();
        Mockito.when(footstepService.getForWearableWithinTimeWindow(
                        Mockito.any(Wearable.class),
                        Mockito.any(TimeWindow.class)
                ))
                .thenReturn(footsteps);
        List<Walk> walkList = rehabilitationAnalysisService.computeWalkTestResults(List.of(tr));
        Walk walk = walkList.get(0);

        // then
        Map<String, Indicator> indicatorsGroupedByName = walk.getIndicators().stream()
                .collect(Collectors.toMap(Indicator::getName, Function.identity()));
        Map<String, Indicator> trialIndicatorsGroupedByName = walk.getTrials().get(0)
                .getIndicators().stream().collect(Collectors.toMap(Indicator::getName, Function.identity()));
        CoveredDistance walkCd = (CoveredDistance) indicatorsGroupedByName.get("CoveredDistance");
        AverageSpeed walkAs = (AverageSpeed) indicatorsGroupedByName.get("AverageSpeed");
        FallRiskScore frsaOfTest = (FallRiskScore) trialIndicatorsGroupedByName.get("FallRiskScore");
        double cdValueOfTest = walkCd.getValue();
        double asValueOfTest = walkAs.getValue();
        assertEquals(cdValue, cdValueOfTest, 0.0);
        assertEquals(asValue, asValueOfTest, 0.0);
        assertEquals(fallRiskScore.getValue(), frsaOfTest.getValue());
    }

    /**
     * Will return a list of footsteps that contains (in terms of the cumulative distance) ten meters of footsteps
     * plus an additional footstep. Used to test the distance and speed values of the ten meter walking test.
     */
    private int findIndexOfFirstTenMetersOfFootsteps(List<Footstep> footsteps) {
        UnitConverter toMeters = MetricPrefix.MILLI(METRE).getConverterTo(METRE);
        int indexOfFootstepThatReachesTenMeters = 0;
        double distance = 0.0;
        while (distance < 10) {
            indexOfFootstepThatReachesTenMeters++;
            CoveredDistance cd = CoveredDistance.of(footsteps.subList(0, indexOfFootstepThatReachesTenMeters));
            distance = toMeters.convert(cd.getValue()).doubleValue();
        }

        /* ASSERTIONS
        When we remove the last footstep from the sub list of footsteps that contains (around) 10 meters, we should end
        up with a list of footsteps that does not reach 10 meters anymore. This way, we know that we found the index of
        the last footstep that completes the list to a list of footsteps reaching the 10 meters threshold.
        */
        List<Footstep> listOfFootstepsWithTenMetersOrABitMore =
                footsteps.subList(0, indexOfFootstepThatReachesTenMeters);
        List<Footstep> listOfFootstepsJustBelowTenMeters =
                footsteps.subList(0, indexOfFootstepThatReachesTenMeters - 1);
        CoveredDistance cdTenMeters = CoveredDistance.of(listOfFootstepsWithTenMetersOrABitMore);
        CoveredDistance cdBelowTenMeters = CoveredDistance.of(listOfFootstepsJustBelowTenMeters);
        boolean listHasEnoughFootstepsForTenMeters = toMeters.convert(cdTenMeters.getValue()).intValue() >= 10;
        boolean listHasNotEnoughFootstepsForTenMeters = toMeters.convert(cdBelowTenMeters.getValue()).intValue() < 10;
        assertTrue(listHasEnoughFootstepsForTenMeters);
        assertTrue(listHasNotEnoughFootstepsForTenMeters);

        return indexOfFootstepThatReachesTenMeters;
    }

    /**
     * Utility method to build a random list of footsteps for use in tests.
     */
    private List<Footstep> buildRandomListOfFootsteps(int amount, Wearable wearable) {
        List<Footstep> randomFootsteps = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Footstep randomFootstep = new Footstep();
            randomFootstep.setFloor(Floor.builder().name("Test").build());
            randomFootstep.setWearable(wearable);
            Position randomPosition = new Position(
                    ThreadLocalRandom.current().nextInt(amount * 2),
                    ThreadLocalRandom.current().nextInt(amount * 2)
            );
            long minDay = LocalDate.of(1970, 1, 1).toEpochDay();
            long maxDay = LocalDate.of(2022, 12, 31).toEpochDay();
            long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
            if (i == 0) {
                randomFootstep.setTime(LocalDateTime.ofEpochSecond(minDay, 0, ZoneOffset.UTC));
            } else if (i == amount - 1) {
                randomFootstep.setTime(LocalDateTime.ofEpochSecond(maxDay, 0, ZoneOffset.UTC));
            } else {
                randomFootstep.setTime(LocalDateTime.ofEpochSecond(randomDay, 0, ZoneOffset.UTC));
            }
            randomFootstep.setPosition(randomPosition);
            randomFootsteps.add(randomFootstep);
        }
        return randomFootsteps;
    }
}
