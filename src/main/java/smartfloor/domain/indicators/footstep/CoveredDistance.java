package smartfloor.domain.indicators.footstep;

import java.util.List;
import java.util.stream.IntStream;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;

/**
 * One basic parameter than can be computed from a given list of footsteps is the covered distance.
 * This is the total covered (i.e. cumulative) distance for a given input list (path) of footsteps.
 */
public final class CoveredDistance implements Indicator {

    private Double value;
    private final String unit = "mm";

    private CoveredDistance() {
    }

    private CoveredDistance(List<Footstep> footsteps) {
        this.value = compute(footsteps);
    }

    private CoveredDistance(Double aggregate) {
        this.value = aggregate;
    }

    public static CoveredDistance of(List<Footstep> footsteps) {
        return new CoveredDistance(footsteps);
    }

    public static CoveredDistance of(Number aggregate) {
        return new CoveredDistance(aggregate.doubleValue());
    }

    @Override
    public String getUnit() {
        return this.unit;
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    private Double compute(List<Footstep> footsteps) {
        final List<Footstep> footstepsWithPosition = footsteps
                .stream()
                .filter(Footstep::hasPosition)
                .toList();
        return IntStream
                .range(0, footstepsWithPosition.size() - 1)
                .mapToDouble(i -> footstepsWithPosition.get(i).getPosition()
                        .distanceTo(
                                footstepsWithPosition.get(i + 1).getPosition())
                )
                .sum();
    }

}
