package smartfloor.domain.tests.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.tests.SixMinuteWalking;
import smartfloor.domain.tests.TenMeterWalking;
import smartfloor.domain.tests.Test;
import smartfloor.domain.tests.TimedUpNGo;
import smartfloor.domain.tests.Walk;

public  class TestUtils {

    /**
     * TODO.
     */
    public static Map<Class<? extends Indicator>,
            List<Double>> groupByIndicator(List<smartfloor.domain.tests.Trial> trials) {
        return trials
                .stream()
                .flatMap(trial -> trial.getIndicators().stream())
                .collect(
                        Collectors.groupingBy(
                                Indicator::getClass,
                                Collectors.mapping(
                                        indicator -> indicator.getValue().doubleValue(),
                                        Collectors.toList()
                                )
                        )
                );
    }

    /**
     * TODO.
     * TODO: Change to trial instead of just its time window (second and third parameters) once TestResult no longer has
     *     a TestTrialForm as its trial attribute.
     */
    public static List<Footstep> sliceFootstepsForTrial(
            List<Footstep> footsteps,
            LocalDateTime trialBeginTime,
            LocalDateTime trialEndTime
    ) {
        return footsteps
                .stream()
                .filter(footstep ->
                        (footstep.getTime().isEqual(trialBeginTime) || footstep.getTime().isAfter(trialBeginTime)) &&
                                (footstep.getTime().isEqual(trialEndTime) || footstep.getTime().isBefore(trialEndTime))
                )
                .toList();
    }

    /**
     * TODO.
     */
    public static Test computeTestFromTestResult(TestResult testResult, List<Footstep> footsteps, UserInfo userInfo) {
        switch (testResult.getType()) {
            case TEN_METER_WALKING:
                return TenMeterWalking.from(testResult, footsteps);
            case WALK:
                return Walk.from(testResult, footsteps);
            case TIMED_UP_N_GO:
                return TimedUpNGo.from(testResult, footsteps);
            case SIX_MINUTE_WALKING:
                return SixMinuteWalking.from(testResult, footsteps, userInfo);
            default:
                throw new IllegalArgumentException("Test type not supported");
        }
    }
}
