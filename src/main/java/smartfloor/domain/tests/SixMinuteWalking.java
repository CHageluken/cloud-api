package smartfloor.domain.tests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import smartfloor.deserializer.SixMinuteWalkingDeserializer;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.indicators.rehabilitation.TargetDistance;
import smartfloor.domain.tests.util.TestUtils;

/**
 * The Six Minute Walking test, based on the similarly named rehabilitation test (protocol).
 * Rehabilitation users are supposed to walk (reach) a target distance within 6 minutes.
 */
@JsonDeserialize(using = SixMinuteWalkingDeserializer.class)
public final class SixMinuteWalking extends AbstractTest {
    private static UserInfo userInfo;

    public SixMinuteWalking(List<smartfloor.domain.tests.Trial> trials) {
        super(trials);
    }

    public SixMinuteWalking(List<smartfloor.domain.tests.Trial> trials, List<Indicator> indicators) {
        super(trials, indicators);
    }

    /**
     * TODO.
     */
    public static SixMinuteWalking from(TestResult testResult, List<Footstep> footsteps, UserInfo userInfo) {
        SixMinuteWalking.userInfo = userInfo;
        return new SixMinuteWalking(
                testResult.getTrials()
                        .stream()
                        .map(trial ->
                                new Trial(
                                        trial.getBeginTime(),
                                        trial.getEndTime(),
                                        TestUtils.sliceFootstepsForTrial(
                                                footsteps,
                                                trial.getBeginTime(),
                                                trial.getEndTime()
                                        )
                                ))
                        .collect(Collectors.toUnmodifiableList())
        );
    }

    @Override
    protected List<Indicator> compute(List<smartfloor.domain.tests.Trial> trials) {
        Map<Class<? extends Indicator>, List<Double>> indicators = TestUtils.groupByIndicator(trials);

        double distance = indicators.get(CoveredDistance.class)
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        double averageSpeed = indicators.get(AverageSpeed.class)
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();
        TargetDistance td = new TargetDistance(userInfo);
        // Set the userInfo to null, since the field is static, and we don't want next instances of the SixMinuteWalking
        // class to be affected by previous ones.
        userInfo = null;
        return List.of(
                CoveredDistance.of(distance),
                AverageSpeed.of(averageSpeed),
                td
        );
    }

    public static final class Trial extends AbstractTest.AbstractTrial {

        public Trial(LocalDateTime beginTime, LocalDateTime endTime, List<Footstep> footsteps) {
            super(beginTime, endTime, footsteps);
        }

        public Trial(List<Indicator> indicators, LocalDateTime beginTime, LocalDateTime endTime) {
            super(indicators, beginTime, endTime);
        }

        @Override
        protected List<Indicator> compute(List<Footstep> footsteps) {
            return List.of(
                    CoveredDistance.of(footsteps),
                    AverageSpeed.of(footsteps)
            );
        }
    }
}
