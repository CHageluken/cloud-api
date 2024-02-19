package smartfloor.domain.indicators.footstep;

import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;

/**
 * The first-to-last step distance is (as the name implies) the (Euclidean) distance from the first to the last footstep
 * of the given list of footsteps. Useful as a distance measure in a limited number of cases.
 */
public final class FirstToLastStepDistance implements Indicator {

    private Double value;
    private final String unit = "mm";

    private FirstToLastStepDistance() {
    }

    public FirstToLastStepDistance(List<Footstep> footsteps) {
        this.value = compute(footsteps);
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    /**
     * Note: We only use footsteps from which a position could be determined (for now).
     * Otherwise, should either the first or last step be missing a position, we cannot correctly compute the distance.
     *
     * @param footsteps an input list (path) of footsteps
     * @return first to last step distance of the list (path) of footsteps in mm
     */
    Double compute(List<Footstep> footsteps) {
        List<Footstep> footstepsWithPosition = footsteps
                .stream()
                .filter(Footstep::hasPosition)
                .toList();
        if (footstepsWithPosition.size() >= 2) {
            Footstep first = footstepsWithPosition.get(0);
            Footstep last = footstepsWithPosition.get(footstepsWithPosition.size() - 1);
            return first.getPosition().distanceTo(last.getPosition());
        }
        return 0.0;
    }

}
