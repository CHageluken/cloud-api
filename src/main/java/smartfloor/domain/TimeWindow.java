package smartfloor.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.Footstep;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * Wrapper object for time windows that are used for session and user-wearable link related things.
 * Consists of a begin timestamp and end timestamp that together define a time window in which for example a session or
 * user-wearable link holds.
 */
public class TimeWindow implements Serializable {

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    public TimeWindow() {
    }

    /**
     * TODO.
     */
    public TimeWindow(long beginTimeInMillis, long endTimeInMillis) {
        beginTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(beginTimeInMillis), ZoneOffset.UTC);
        endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeInMillis), ZoneOffset.UTC);
        if (beginTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Illegal time window: the specified begin time is after the end time.");
        }
    }

    /**
     * TODO.
     */
    public TimeWindow(LocalDateTime beginTime, LocalDateTime endTime) {
        this.beginTime = beginTime;
        this.endTime = endTime;
        if (beginTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Illegal time window: the specified begin time is after the end time.");
        }
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean includes(Footstep f) {
        return (f.getTime().isEqual(this.beginTime) || f.getTime().isAfter(this.beginTime)) &&
                (f.getTime().isEqual(this.endTime) || f.getTime().isBefore(this.endTime));
    }

    public boolean includes(LocalDateTime beginTime, LocalDateTime endTime) {
        return (beginTime.isEqual(this.beginTime) || beginTime.isAfter(this.beginTime)) &&
                (endTime.isEqual(this.endTime) || endTime.isBefore(this.endTime));
    }

    /**
     * Returns a time window based on a list of footsteps.
     *
     * @param footsteps a list of footsteps
     * @return a time window that spans from the first footstep's timestamp to the last footstep's timestamp
     */
    public static TimeWindow fromFootsteps(List<Footstep> footsteps) {
        return new TimeWindow(footsteps.get(0).getTime(), footsteps.get(footsteps.size() - 1).getTime());
    }

    /**
     * TODO.
     */
    public static TimeWindow minimum(TimeWindow t1, TimeWindow t2) {
        LocalDateTime begin = t1.getBeginTime().isBefore(t2.getBeginTime()) ? t2.getBeginTime() : t1.getBeginTime();
        LocalDateTime end = t1.getEndTime().isAfter(t2.getEndTime()) ? t2.getEndTime() : t1.getEndTime();
        return new TimeWindow(begin, end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeWindow that = (TimeWindow) o;
        return Objects.equals(beginTime, that.beginTime) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginTime, endTime);
    }
}
