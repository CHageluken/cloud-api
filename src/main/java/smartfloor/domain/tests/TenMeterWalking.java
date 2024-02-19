package smartfloor.domain.tests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.measure.MetricPrefix;
import javax.measure.UnitConverter;
import smartfloor.deserializer.TenMeterWalkingDeserializer;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.tests.util.TestUtils;
import static tech.units.indriya.unit.Units.METRE;

/**
 * The Ten Meter Walking test (10MWT) is supposed to test the physical condition of a rehabilitation user over a
 * distance of 10 meters. The analysis is therefore strictly over the first 10 meters of the footsteps provided to this
 * instance.
 */
@JsonDeserialize(using = TenMeterWalkingDeserializer.class)
public final class TenMeterWalking extends AbstractTest {

    public TenMeterWalking(List<smartfloor.domain.tests.Trial> trials) {
        super(trials);
    }

    public TenMeterWalking(List<smartfloor.domain.tests.Trial> trials, List<Indicator> indicators) {
        super(trials, indicators);
    }

    /**
     * TODO.
     */
    public static TenMeterWalking from(TestResult testResult, List<Footstep> footsteps) {
        return new TenMeterWalking(
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

        double averageSpeed = indicators.get(AverageSpeed.class)
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();

        return List.of(
                AverageSpeed.of(averageSpeed)
        );
    }

    public static final class Trial extends AbstractTrial {

        private static List<Footstep> findFirstTenMeters(List<Footstep> footsteps) {
            final List<Footstep> footstepsWithPosition = footsteps
                    .stream()
                    .filter(Footstep::hasPosition)
                    .toList();
            Double distance = 0.0;
            UnitConverter toMeters = MetricPrefix.MILLI(METRE).getConverterTo(METRE);
            for (int i = 0; i < footstepsWithPosition.size() - 1; i++) {
                distance += footstepsWithPosition.get(i)
                        .getPosition()
                        .distanceTo(footstepsWithPosition.get(i + 1).getPosition());
                final double distanceInMeters = toMeters.convert(distance).doubleValue();
                /* We find the sub list of footsteps that has at least 10 meters (the last footstep makes it 10 meters
                 or a bit more). Sub list includes from 0 to i + 1 since we need to include the step beyond this
                 iteration since that is what we take the last distance to (i+2 since the endpoint is exclusive). */
                if (distanceInMeters >= 10) return footstepsWithPosition.subList(0, i + 2);
            }
            return footstepsWithPosition; // <= 10 meters
        }

        public Trial(LocalDateTime beginTime, LocalDateTime endTime, List<Footstep> footsteps) {
            super(beginTime, endTime, findFirstTenMeters(footsteps));
        }

        public Trial(List<Indicator> indicators, LocalDateTime beginTime, LocalDateTime endTime) {
            super(indicators, beginTime, endTime);
        }

        @Override
        protected List<Indicator> compute(List<Footstep> footsteps) {
            return List.of(
                    AverageSpeed.of(footsteps)
            );
        }

    }

}
