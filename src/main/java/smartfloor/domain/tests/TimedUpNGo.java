package smartfloor.domain.tests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import smartfloor.deserializer.TimedUpNGoDeserializer;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.indicators.footstep.WalkingTime;
import smartfloor.domain.tests.util.TestUtils;

/**
 * The Timed Up N Go test based on the rehabilitation test (protocol) with the same name.
 */
@JsonDeserialize(using = TimedUpNGoDeserializer.class)
public final class TimedUpNGo extends AbstractTest {

    public TimedUpNGo(List<smartfloor.domain.tests.Trial> trials) {
        super(trials);
    }

    public TimedUpNGo(List<smartfloor.domain.tests.Trial> trials, List<Indicator> indicators) {
        super(trials, indicators);
    }

    /**
     * TODO.
     */
    public static TimedUpNGo from(TestResult testResult, List<Footstep> footsteps) {
        return new TimedUpNGo(
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
        return List.of(
                WalkingTime.of(super.getBeginTime(), super.getEndTime())
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
                    WalkingTime.of(super.getBeginTime(), super.getEndTime())
            );
        }
    }
}
