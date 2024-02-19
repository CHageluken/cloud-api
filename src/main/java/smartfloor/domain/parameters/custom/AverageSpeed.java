package smartfloor.domain.parameters.custom;

import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.parameters.Parameter;
import smartfloor.domain.parameters.footstep.DistanceMeasure;

/**
 * A custom average speed parameter class that allows for the computation of different instances of the average speed.
 * These instances can consist of ones where the walking time and/or distance is provided separately instead of being
 * based on the set of footsteps associated with the average speed.
 */
public class AverageSpeed extends Parameter<Double> {
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getUnit() {
        return "mm/s";
    }

    private AverageSpeed(Double distanceInMillimeters, Double walkingTimeInSeconds) {
        this.setValue(distanceInMillimeters / walkingTimeInSeconds);
    }

    private AverageSpeed() {
    }

    /**
     * Uses the default covered distance implementation for computing distance from a given list of footsteps.
     * Accepts a walking time object.
     */
    public static AverageSpeed withWalkingTime(List<Footstep> footsteps, WalkingTime walkingTime) {
        CoveredDistance cd = CoveredDistance.of(footsteps);
        return new AverageSpeed(cd.getValue(), walkingTime.getValue());
    }

    /**
     * Uses a custom distance measure implementation (assumption: distance provided in mm as is the default).
     * TODO: #393 Find better way of handling different distance measures or choose a good default (possibly based on
     * checking the path of footsteps first).
     * Uses default walking time (based on footsteps).
     */
    public static AverageSpeed withDistance(List<Footstep> footsteps, DistanceMeasure<Double> distance) {
        if (footsteps.isEmpty()) return new AverageSpeed(distance.getValue(), 0.0);
        smartfloor.domain.indicators.footstep.WalkingTime wt =
                smartfloor.domain.indicators.footstep.WalkingTime.of(footsteps);
        return new AverageSpeed(distance.getValue(), wt.getValue());
    }

    /**
     * Uses a custom distance measure implementation (assumption: distance provided in mm as is the default).
     * TODO: #393 Find better way of handling different distance measures or choose a good default (possibly based on
     * checking the path of footsteps first).
     * Accepts a walking time object.
     */
    public static AverageSpeed withDistanceAndWalkingTime(DistanceMeasure<Double> distance, WalkingTime walkingTime) {
        return new AverageSpeed(distance.getValue(), walkingTime.getValue());
    }

    @Override
    public String toString() {
        return "AverageSpeed{" +
                "value=" + getValue() +
                '}';
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
