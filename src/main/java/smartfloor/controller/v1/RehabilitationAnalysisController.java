package smartfloor.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.domain.tests.SixMinuteWalking;
import smartfloor.domain.tests.TenMeterWalking;
import smartfloor.domain.tests.TimedUpNGo;
import smartfloor.domain.tests.Walk;
import smartfloor.service.RehabilitationAnalysisService;
import smartfloor.service.RehabilitationService;
import smartfloor.service.UserService;
import smartfloor.service.WearableService;

@Tag(
        name = "Rehabilitation analysis API",
        description = "Provides various rehabilitation analyses based on footstep data."
)
@RestController
@RequestMapping("/v1/analyses/rehabilitation")
public class RehabilitationAnalysisController {
    private final RehabilitationAnalysisService rehabilitationAnalysisService;
    private final RehabilitationService rehabilitationService;
    private final UserService userService;
    private final WearableService wearableService;

    /**
     * TODO.
     */
    @Autowired
    public RehabilitationAnalysisController(
            RehabilitationAnalysisService rehabilitationAnalysisService,
            RehabilitationService rehabilitationService,
            UserService userService,
            WearableService wearableService
    ) {
        this.rehabilitationAnalysisService = rehabilitationAnalysisService;
        this.rehabilitationService = rehabilitationService;
        this.userService = userService;
        this.wearableService = wearableService;
    }

    /**
     * An ad-hoc endpoint that allows one to compute a ten-meter walking test for an arbitrary wearable id and time
     * window.
     */
    @Operation(description = "Get an ad-hoc ten meter walking test result for a given wearable id and time window.")
    @GetMapping("/tests/ten-meter-walking/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public TenMeterWalking getTenMeterWalkingTestForWearableWithinTimeWindow(
            @PathVariable(value = "wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return rehabilitationAnalysisService.computeTenMeterWalkingTestForWearableWithinTimeWindow(
                wearable,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * TODO.
     */
    @Operation(description = "Get all Ten Meter Walking test results for a given user and time window.")
    @GetMapping("/tests/ten-meter-walking/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TenMeterWalking> getTenMeterWalkingTestResultsForUserWithinTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        TimeWindow timeWindow = new TimeWindow(beginTime, endTime);
        List<TestResult> testResults =
                rehabilitationService.getTestResultsOfTypeForUserWithinTimeWindow(
                        TestType.TEN_METER_WALKING,
                        user,
                        timeWindow
                );
        return rehabilitationAnalysisService.computeTenMeterWalkingTestResults(testResults);
    }

    /**
     * TODO.
     */
    @GetMapping("/tests/timed-up-and-go/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TimedUpNGo> getTimedUpNGoTestResultsForUserWithinTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        TimeWindow timeWindow = new TimeWindow(beginTime, endTime);
        List<TestResult> testResults =
                rehabilitationService.getTestResultsOfTypeForUserWithinTimeWindow(
                        TestType.TIMED_UP_N_GO,
                        user,
                        timeWindow
                );
        return rehabilitationAnalysisService.computeTimedUpNGoTestResults(testResults);
    }

    /**
     * An ad-hoc endpoint that allows one to compute a six minute walking test result for an arbitrary wearable id and
     * time window.
     * Note: This response will contain the TargetDistance indicator, but its value will be 0, since we are not sure
     * which user is associated with the provided wearable.
     */
    @Operation(description = "Get an ad-hoc six minute walking test result for a given wearable id and time window.")
    @GetMapping("/tests/six-minute-walking/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public SixMinuteWalking getSixMinuteWalkingTestForWearableWithinTimeWindow(
            @PathVariable(value = "wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return rehabilitationAnalysisService.computeSixMinuteWalkingTestForWearableWithinTimeWindow(
                wearable,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * TODO.
     */
    @GetMapping("/tests/six-minute-walking/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<SixMinuteWalking> getSixMinuteWalkingTestResultsForUserWithinTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        TimeWindow timeWindow = new TimeWindow(beginTime, endTime);
        List<TestResult> testResults =
                rehabilitationService.getTestResultsOfTypeForUserWithinTimeWindow(
                        TestType.SIX_MINUTE_WALKING,
                        user,
                        timeWindow
                );
        return rehabilitationAnalysisService.computeSixMinuteWalkingTestResults(testResults);
    }

    /**
     * TODO.
     */
    @Operation(description = "Get all Walk test results for a given user and time window.")
    @GetMapping("/tests/walk/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<Walk> getWalkTestResultsForUserWithinTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        TimeWindow timeWindow = new TimeWindow(beginTime, endTime);
        List<TestResult> testResults =
                rehabilitationService.getTestResultsOfTypeForUserWithinTimeWindow(TestType.WALK, user, timeWindow);
        return rehabilitationAnalysisService.computeWalkTestResults(testResults);
    }

    /**
     * An ad-hoc endpoint that allows one to compute a walk test result for an arbitrary wearable id and time window.
     */
    @Operation(description = "Get an ad-hoc walk test result for a given wearable id and time window.")
    @GetMapping("/tests/walk/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public Walk getWalkTestForWearableWithinTimeWindow(
            @PathVariable(value = "wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return rehabilitationAnalysisService.computeWalkTestForWearableWithinTimeWindow(
                wearable,
                new TimeWindow(beginTime, endTime)
        );
    }
}
