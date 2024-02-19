package smartfloor.domain.parameters.custom;

import java.time.ZoneOffset;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.parameters.Parameter;

/**
 * Normally, the WalkingTime class can be used to determine the walking time from a given set of footsteps.
 * However, in some cases we want to specify a custom time window that determines the walking time for a user
 * (in seconds).
 */
public class WalkingTime extends Parameter<Double> {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getUnit() {
        return "s";
    }

    /**
     * TODO.
     */
    public WalkingTime(TimeWindow timeWindow) {
        final long beginTime = timeWindow.getBeginTime().toInstant(ZoneOffset.UTC).toEpochMilli();
        final long endTime = timeWindow.getEndTime().toInstant(ZoneOffset.UTC).toEpochMilli();
        this.setValue((endTime - beginTime) / 1000.0);
    }
}
