package smartfloor.domain.tests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

public abstract class AbstractTest implements Test {

    private final List<smartfloor.domain.tests.Trial> trials;

    private final List<Indicator> indicators;

    protected AbstractTest(List<Trial> trials) {
        this.trials = trials;
        this.indicators = compute(trials);
    }

    protected AbstractTest(List<Trial> trials, List<Indicator> indicators) {
        this.trials = trials;
        this.indicators = indicators;
    }

    @Override
    public List<Trial> getTrials() {
        return trials;
    }

    @Override
    public List<Indicator> getIndicators() {
        return indicators;
    }

    /**
     * TODO.
     */
    public Indicator getIndicatorByName(String name) {
        List<Indicator> filteredIndicators = this.indicators.stream()
                .filter(indicator -> Objects.equals(indicator.getName(), name))
                .toList();
        if (!filteredIndicators.isEmpty()) {
            return filteredIndicators.get(0);
        }
        return null;
    }

    protected abstract List<Indicator> compute(List<smartfloor.domain.tests.Trial> trials);

    public abstract static class AbstractTrial implements smartfloor.domain.tests.Trial {

        @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
        @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
        private final LocalDateTime beginTime;

        @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
        @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
        private final LocalDateTime endTime;

        @JsonIgnore
        private List<Footstep> footsteps;

        private final List<Indicator> indicators;

        /**
         * TODO.
         */
        protected AbstractTrial(LocalDateTime beginTime, LocalDateTime endTime, List<Footstep> footsteps) {
            this.beginTime = beginTime;
            this.endTime = endTime;
            this.footsteps = Collections.unmodifiableList(footsteps);
            this.indicators = compute(footsteps);
        }

        /**
         * TODO.
         */
        protected AbstractTrial(List<Indicator> indicators, LocalDateTime beginTime, LocalDateTime endTime) {
            this.indicators = indicators;
            this.beginTime = beginTime;
            this.endTime = endTime;
        }

        protected abstract List<Indicator> compute(List<Footstep> footsteps);

        @Override
        public LocalDateTime getBeginTime() {
            return beginTime;
        }

        @Override
        public LocalDateTime getEndTime() {
            return endTime;
        }

        @Override
        public List<Footstep> getFootsteps() {
            return footsteps;
        }

        @Override
        public List<Indicator> getIndicators() {
            return indicators;
        }

        @Override
        public Indicator getIndicatorByName(String name) {
            List<Indicator> filteredIndicators = this.indicators.stream()
                    .filter(indicator -> Objects.equals(indicator.getName(), name))
                    .toList();
            if (!filteredIndicators.isEmpty()) {
                return filteredIndicators.get(0);
            }
            return null;
        }
    }

}
