package smartfloor.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.WearableWithSide;
import smartfloor.domain.entities.user.info.history.UserInfoHistory;
import smartfloor.domain.tests.SixMinuteWalking;
import smartfloor.domain.tests.TenMeterWalking;
import smartfloor.domain.tests.TimedUpNGo;
import smartfloor.domain.tests.Walk;
import smartfloor.repository.jpa.UserInfoHistoryRepository;

@Service
public class RehabilitationAnalysisService {

    private final FootstepService footstepService;
    private final UserInfoHistoryRepository userInfoHistoryRepository;

    /**
     * TODO.
     */
    @Autowired
    public RehabilitationAnalysisService(
            FootstepService footstepService,
            UserInfoHistoryRepository userInfoHistoryRepository
    ) {
        this.footstepService = footstepService;
        this.userInfoHistoryRepository = userInfoHistoryRepository;
    }


    /**
     * <p>Compute the Ten Meter Walking test for a wearable within a time window.</p>
     * Note: technically a single trial of a ten meter walking test. A ten meter walking test can have three trials,
     * each with their own set of footsteps (for a wearable and time window).
     */
    public TenMeterWalking computeTenMeterWalkingTestForWearableWithinTimeWindow(
            Wearable wearable,
            TimeWindow timeWindow
    ) {
        List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        TenMeterWalking.Trial trial =
                new TenMeterWalking.Trial(timeWindow.getBeginTime(), timeWindow.getEndTime(), footsteps);
        return new TenMeterWalking(List.of(trial));
    }

    /**
     * TODO.
     */
    public List<TenMeterWalking> computeTenMeterWalkingTestResults(List<TestResult> testResults) {
        List<TenMeterWalking> tenMeterWalkingTests = new ArrayList<>();
        for (TestResult testResult : testResults) {
            TimeWindow timeWindow = new TimeWindow(testResult.getBeginTime(), testResult.getEndTime());
            // Technically, a wearable is always present for 10MWTs.
            WearableWithSide wearableWithSide = testResult.getWearableWithSide();
            if (wearableWithSide != null) {
                Wearable wearable = wearableWithSide.getWearable();
                if (wearable != null) {
                    List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
                    // Only return a ten meter walking result when footsteps are present. TODO: #449.
                    if (!footsteps.isEmpty()) {
                        TenMeterWalking tmwt = TenMeterWalking.from(testResult, footsteps);
                        tenMeterWalkingTests.add(tmwt);
                    }
                }
            }
        }
        return tenMeterWalkingTests;
    }

    /**
     * TODO.
     */
    public List<TimedUpNGo> computeTimedUpNGoTestResults(List<TestResult> testResults) {
        List<TimedUpNGo> timedUpNGoTests = new ArrayList<>();
        for (TestResult testResult : testResults) {
            TimedUpNGo tung = TimedUpNGo.from(testResult, new ArrayList<>());
            timedUpNGoTests.add(tung);
        }
        return timedUpNGoTests;
    }

    /**
     * TODO.
     */
    public SixMinuteWalking computeSixMinuteWalkingTestForWearableWithinTimeWindow(
            Wearable wearable,
            TimeWindow timeWindow
    ) {
        List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        SixMinuteWalking.Trial trial =
                new SixMinuteWalking.Trial(timeWindow.getBeginTime(), timeWindow.getEndTime(), footsteps);
        return new SixMinuteWalking(List.of(trial));
    }

    /**
     * TODO.
     */
    public List<SixMinuteWalking> computeSixMinuteWalkingTestResults(List<TestResult> testResults) {
        List<SixMinuteWalking> sixMinuteWalkingTests = new ArrayList<>();
        for (TestResult testResult : testResults) {
            TimeWindow timeWindow = new TimeWindow(testResult.getBeginTime(), testResult.getEndTime());
            WearableWithSide wearableWithSide = testResult.getWearableWithSide();
            if (wearableWithSide != null) {
                Wearable wearable = wearableWithSide.getWearable();
                if (wearable != null) {
                    List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
                    // Only return a ten meter walking result when footsteps are present. TODO: #449.
                    if (!footsteps.isEmpty()) {
                        SixMinuteWalking smwt = SixMinuteWalking.from(
                                testResult,
                                footsteps,
                                getUserInfoForUserAndTestResult(testResult.getUser(), testResult)
                        );
                        sixMinuteWalkingTests.add(smwt);
                    }
                }
            }
        }
        return sixMinuteWalkingTests;
    }

    /**
     * TODO.
     */
    public Walk computeWalkTestForWearableWithinTimeWindow(Wearable wearable, TimeWindow timeWindow) {
        List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        Walk.Trial trial = new Walk.Trial(timeWindow.getBeginTime(), timeWindow.getEndTime(), footsteps);
        return new Walk(List.of(trial));
    }

    /**
     * TODO.
     */
    public List<Walk> computeWalkTestResults(List<TestResult> testResults) {
        List<Walk> walkTests = new ArrayList<>();
        for (TestResult testResult : testResults) {
            TimeWindow timeWindow = new TimeWindow(testResult.getBeginTime(), testResult.getEndTime());
            WearableWithSide wearableWithSide = testResult.getWearableWithSide();
            if (wearableWithSide != null) {
                Wearable wearable = wearableWithSide.getWearable();
                if (wearable != null) {
                    List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
                    if (!footsteps.isEmpty()) {
                        Walk wt = Walk.from(testResult, footsteps);
                        walkTests.add(wt);
                    }
                }
            }
        }
        return walkTests;
    }

    public UserInfo getUserInfoForUserAndTestResult(User user, TestResult testResult) {
        return getEarliestUserInfoAfterTime(user, testResult.getBeginTime());
    }

    /**
     * Looks up the earliest UserInfoHistory after a specific point. If no history is found, the current user's UserInfo
     * is returned. If this also isn't present, we return an empty UserInfo.
     */
    public UserInfo getEarliestUserInfoAfterTime(User user, LocalDateTime timestamp) {
        Optional<UserInfoHistory> userInfoHistory =
                userInfoHistoryRepository.findEarliestByUserIdAfterEndTime(user.getId(), timestamp);
        if (userInfoHistory.isPresent()) {
            return userInfoHistory.get().getInfo();
        } else if (user.getInfo() != null) {
            return user.getInfo();
        }
        return new UserInfo();
    }
}
