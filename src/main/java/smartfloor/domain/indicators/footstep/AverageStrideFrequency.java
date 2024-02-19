package smartfloor.domain.indicators.footstep;

import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;

public final class AverageStrideFrequency implements Indicator {

    private Double value;
    private final String unit = "steps/second";

    private AverageStrideFrequency() {
    }

    private AverageStrideFrequency(List<Footstep> footsteps) {
        this.value = compute(footsteps);
    }

    private AverageStrideFrequency(Double value) {
        this.value = value;
    }

    public static AverageStrideFrequency of(List<Footstep> footsteps) {
        return new AverageStrideFrequency(footsteps);
    }

    public static AverageStrideFrequency of(Number aggregate) {
        return new AverageStrideFrequency(aggregate.doubleValue());
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public String getUnit() {
        return this.unit;
    }

    /**
     * <p>The average step frequency for a given list (path) of footsteps is determined by taking the amount of steps
     * and dividing it by the time it took to walk the path (i.e. the time between the first and last footstep).</p>
     * NOTE: When supplying a list of footsteps from a SINGLE wearable, we should consider this the average STRIDE
     * frequency.
     *
     * @param footsteps an input list (path) of footsteps
     * @return the average step frequency in steps/second
     */
    private Double compute(List<Footstep> footsteps) {
        if (!footsteps.isEmpty()) {
            final int amountOfSteps = footsteps.size();
            WalkingTime walkingTime = WalkingTime.of(footsteps);
            return amountOfSteps / walkingTime.getValue();
        }
        return 0.0;
    }
}
