package smartfloor.domain.indicators.footstep;

import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;

public final class AverageSpeed implements Indicator {

    private Double value;
    private final String unit = "mm/s";

    private AverageSpeed() {
    }

    private AverageSpeed(List<Footstep> footsteps) {
        this.value = compute(footsteps);
    }

    private AverageSpeed(Double aggregate) {
        this.value = aggregate;
    }

    private AverageSpeed(List<Footstep> footsteps, WalkingTime time) {
        this.value = compute(footsteps, time);
    }

    public static AverageSpeed of(List<Footstep> footsteps) {
        return new AverageSpeed(footsteps);
    }

    public static AverageSpeed of(Number aggregate) {
        return new AverageSpeed(aggregate.doubleValue());
    }

    public static AverageSpeed withWalkingTime(List<Footstep> footsteps, WalkingTime time) {
        return new AverageSpeed(footsteps, time);
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public String getUnit() {
        return this.unit;
    }

    private Double compute(List<Footstep> footsteps, WalkingTime time) {
        CoveredDistance distance = CoveredDistance.of(footsteps);
        if (time.getValue() > 0) {
            return distance.getValue() / time.getValue();
        }
        return 0.0;
    }

    private Double compute(List<Footstep> footsteps) {
        WalkingTime time = WalkingTime.of(footsteps);
        return compute(footsteps, time);
    }

}
