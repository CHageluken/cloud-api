package smartfloor.domain.indicators.footstep;

import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;

public final class AverageStrideLength implements Indicator {

    private Double value;
    private final String unit = "mm";

    private AverageStrideLength() {
    }

    private AverageStrideLength(Double value) {
        this.value = value;
    }

    private AverageStrideLength(List<Footstep> footsteps) {
        this.value = compute(footsteps);
    }

    public static AverageStrideLength of(List<Footstep> footsteps) {
        return new AverageStrideLength(footsteps);
    }

    /**
     * Creates a StrideLength indicator (in mm, for now) from an existing stride length (in mm).
     */
    public static AverageStrideLength of(Number aggregate) {
        return new AverageStrideLength(aggregate.doubleValue());
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public String getUnit() {
        return this.unit;
    }

    private Double compute(List<Footstep> footsteps) {
        CoveredDistance cd = CoveredDistance.of(footsteps);
        final int amountOfSteps = footsteps.size();
        if (amountOfSteps == 0) {
            return 0.0;
        }
        return cd.getValue() / amountOfSteps;
    }

}
