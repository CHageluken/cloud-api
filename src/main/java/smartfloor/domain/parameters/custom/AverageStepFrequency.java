package smartfloor.domain.parameters.custom;

import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.parameters.Parameter;

/**
 * A custom average step frequency parameter class that allows for the computation of different instances of the
 * average step frequency. These instances can consist of ones where the walking time is provided separately.
 */
public class AverageStepFrequency extends Parameter<Double> {
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getUnit() {
        return "steps/second";
    }

    private AverageStepFrequency(Integer amountOfSteps, Double walkingTimeInSeconds) {
        this.setValue((double) amountOfSteps / walkingTimeInSeconds);
    }

    /**
     * Accepts a walking time object.
     */
    public static AverageStepFrequency withWalkingTime(List<Footstep> footsteps, WalkingTime walkingTime) {
        return new AverageStepFrequency(footsteps.size(), walkingTime.getValue());
    }

}
