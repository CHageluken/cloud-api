package smartfloor.domain.indicators.footstep;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;

public final class WalkingTime implements Indicator {

    private Double value;
    private  final String unit = "s";

    private WalkingTime() {
    }

    private WalkingTime(List<Footstep> footsteps) {
        this.value = compute(footsteps);
    }

    private WalkingTime(Double value) {
        this.value = value;
    }

    /**
     * TODO.
     */
    public static WalkingTime of(List<Footstep> footsteps) {
        return new WalkingTime(footsteps);
    }

    /**
     * TODO.
     */
    public static WalkingTime of(LocalDateTime manualBeginTime, LocalDateTime manualEndTime) {
        final long beginTime = manualBeginTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        final long endTime = manualEndTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        return new WalkingTime((endTime - beginTime) / 1000.0);
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public String getUnit() {
        return this.unit;
    }

    Double compute(List<Footstep> footsteps) {
        if (!footsteps.isEmpty()) {
            final long beginTime = footsteps.get(0).getTime().toInstant(ZoneOffset.UTC).toEpochMilli();
            final long endTime = footsteps.get(footsteps.size() - 1).getTime().toInstant(ZoneOffset.UTC).toEpochMilli();
            return (endTime - beginTime) / 1000.0;
        }
        return 0.0;
    }
}
