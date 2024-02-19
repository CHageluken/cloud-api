package smartfloor.domain.parameters.custom;

import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.parameters.Parameter;
import smartfloor.domain.parameters.footstep.DistanceMeasure;

/**
 * A custom average step length parameter class that allows for the computation of different instances of the average
 * step length. These instances can consist of ones where the distance (measure) is provided separately.
 */
public class AverageStepLength extends Parameter<Double> {
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getUnit() {
        return "mm";
    }

    private AverageStepLength(Double distanceInMillimeters, Integer amountOfSteps) {
        this.setValue(distanceInMillimeters / amountOfSteps);
    }

    /**
     * Allow for a custom distance (measure) to be provided.
     * TODO: #393 Find better way of handling different distance measures or choose a good default (possibly based on
     * checking the path of footsteps first).
     */
    public static AverageStepLength withDistance(List<Footstep> footsteps, DistanceMeasure<Double> distance) {
        return new AverageStepLength(distance.getValue(), footsteps.size());
    }

}
