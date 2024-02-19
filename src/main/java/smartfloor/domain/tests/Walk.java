package smartfloor.domain.tests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import smartfloor.deserializer.WalkDeserializer;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.indicators.fall.risk.FallRiskScore;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.indicators.footstep.WalkingTime;
import smartfloor.domain.tests.util.TestUtils;

/**
 * A general walking test.
 * Similar to the Ten Meter Walking test (10MWT) but without a strict distance limit/threshold.
 */
@JsonDeserialize(using = WalkDeserializer.class)
public class Walk extends AbstractTest {

    public Walk(List<smartfloor.domain.tests.Trial> trials) {
        super(trials);
    }

    public Walk(List<smartfloor.domain.tests.Trial> trials, List<Indicator> indicators) {
        super(trials, indicators);
    }

    /**
     * TODO.
     */
    public static Walk from(TestResult testResult, List<Footstep> footsteps) {
        return new Walk(
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

        return List.of(
                CoveredDistance.of(distance),
                AverageSpeed.of(averageSpeed)
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
                    AverageSpeed.withWalkingTime(footsteps, WalkingTime.of(this.getBeginTime(), this.getEndTime())),
                    FallRiskScore.of(footsteps, this.getBeginTime(), this.getEndTime())
            );
        }
    }
}
