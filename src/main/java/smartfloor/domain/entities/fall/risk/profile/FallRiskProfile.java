package smartfloor.domain.entities.fall.risk.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.lang.Nullable;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.AverageStrideFrequency;
import smartfloor.domain.indicators.footstep.AverageStrideLength;
import smartfloor.serializer.CustomLocalDateTimeSerializer;
import smartfloor.serializer.views.Views;

/**
 * <p>The fall risk profile (FRP) entity contains the values for the features/indicators we deem relevant to
 * determining the fall risk of a user. Example indicators include walking speed, stride length and stride frequency. In
 * principle, a fall risk profile is computed for a single wearable worn by a user within a given timeframe. It can be
 * used to assess a user's fall risk by using its values to determine a total metric, for more info see the note
 * below.</p> NOTE: In the domain terminology, the term "fall risk profile" (FRP) actually refers to the full
 * assessment, including (at least) a total FRP metric that captures the "overall" FRP (such as a score, a risk group
 * etc.). The FallRiskProfile object here does not have such a total metric yet because the particular metric we want to
 * show to the users is subject to change. Hence, we want to compute it (and any other relevant "extra" metrics) as part
 * of a fall risk "assessment". For the latter, see the FallRiskAssessmentModel interface and its implementations.
 */
@Entity
@Table(name = "fall_risk_profiles")
public class FallRiskProfile implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    /**
     * References the wearable that collected the fall risk parameters.
     */
    @ManyToOne
    @JoinColumn(name = "wearable_id", nullable = false)
    @JsonIgnoreProperties({"user", "userWearableLinks"})
    private Wearable wearable;

    /**
     * References the floor on which the fall risk profile was determined.
     */
    @ManyToOne
    @JoinColumn(name = "floor_id")
    @JsonIgnoreProperties({"maxX", "maxY", "orientationNorth", "rotation", "viewers"})
    private Floor floor;

    /**
     * When an FRP is created, we take the timestamp of creation and make it available as the creation time property.
     * This is a useful property to query on when we are looking for FRPs made within a certain time period.
     */
    @Column(name = "created_at")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime creationTime;

    /**
     * The begin time property of an FRP is the timestamp of the first footstep in the sequence of the footsteps that
     * the FRP is based on. Useful when we want to look up the original sequence of footsteps that led to the FRP.
     */
    @Column(name = "begin_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    /**
     * The end time property of an FRP is the timestamp of the last footstep in the sequence of the footsteps that the
     * FRP is based on. Useful when we want to look up the original sequence of footsteps that led to the FRP.
     */
    @Column(name = "end_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    /**
     * <p>The hidden field is a marker/flag field for marking the FRP as hidden or not.</p>
     * <p>Marking an FRP as hidden can be useful for certain FRPs that may have been
     * invalid in terms of their underlying footsteps. This can happen when the footstep (data) selection algorithm is
     * not selective enough during the creation of a certain sequence of footsteps for a user.</p> When marked hidden,
     * it should (by default) not be returned in any result sets obtained using the respective repository for this
     * entity. In some cases, such as for investigation/analysis, one may want to return hidden FRPs as well.
     */
    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    /**
     * <p>The notes field is a field for keeping any remarks on the FRP.</p>
     * Example: If an FRP originates from a post-processing process that tries to find FRPs outside the regular
     * constraints (ex. 4m instead of 5m distance) then it may leave a note/remark in the notes field (such as "4m").
     */
    @Column(name = "notes")
    @JsonIgnore
    private String notes = "";

    /**
     * <p>The speed with which a person was walking during the sequence of footsteps captured for the wearable device
     * within the period [beginTime, endTime].</p> Unit: mm/s
     */
    @Column(name = "walking_speed", nullable = false)
    private Double walkingSpeed;

    /**
     * <p>Currently this is called step length but technically it should be called STRIDE length if the FRP concerns
     * a single wearable.
     * TODO: We have to decide what to call this field (and the db column) once we will (additionally) provide FRPs for
     * two wearables.</p>
     * Unit: mm
     */
    @Column(name = "step_length", nullable = false)
    private Double stepLength;

    /**
     * <p>Currently this is called step frequency but technically it should be called STRIDE frequency if the FRP
     * concerns a single wearable. We have to decide what to call this field (and the db column) once we will
     * (additionally) provide FRPs for two wearables.</p> Unit: steps (strides)/s
     */
    @Column(name = "step_frequency", nullable = false)
    private Double stepFrequency;

    @Column(name = "rms_vertical_accel")
    @JsonIgnore
    private Double rmsVerticalAcceleration;

    @OneToOne(mappedBy = "fallRiskProfile", cascade = CascadeType.ALL)
    @Nullable
    @JsonView(Views.Manager.class)
    private FallRiskProfileNote fallRiskProfileNote;

    protected FallRiskProfile() {
    }

    @Builder
    private FallRiskProfile(
            Floor floor,
            Wearable wearable,
            LocalDateTime creationTime,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            Double walkingSpeed,
            Double stepLength,
            Double stepFrequency,
            Double rmsVerticalAcceleration,
            boolean hidden,
            String notes,
            FallRiskProfileNote fallRiskProfileNote
    ) {
        this.floor = floor;
        this.wearable = wearable;
        this.creationTime = creationTime;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.walkingSpeed = walkingSpeed;
        this.stepLength = stepLength;
        this.stepFrequency = stepFrequency;
        this.rmsVerticalAcceleration = rmsVerticalAcceleration;
        this.hidden = hidden;
        this.notes = notes;
        this.fallRiskProfileNote = fallRiskProfileNote;
    }

    /**
     * Construct an FRP from a given set of footsteps.
     *
     * @return an FRP based on the given set of footsteps.
     */
    public static FallRiskProfile fromFootsteps(List<Footstep> footsteps) {
        List<Footstep> footstepsWithPosition =
                footsteps.stream().filter(Footstep::hasPosition).toList();
        if (!footstepsWithPosition.isEmpty()) {
            Floor floor = footstepsWithPosition.get(0).getFloor();
            Wearable wearable = footstepsWithPosition.get(0).getWearable();
            LocalDateTime beginTime = footstepsWithPosition.get(0).getTime();
            LocalDateTime endTime = footstepsWithPosition.get(footstepsWithPosition.size() - 1).getTime();
            AverageSpeed speed = AverageSpeed.of(footstepsWithPosition);
            AverageStrideLength strideLength = AverageStrideLength.of(footstepsWithPosition);
            AverageStrideFrequency strideFrequency = AverageStrideFrequency.of(footstepsWithPosition);
            return new FallRiskProfile(
                    floor,
                    wearable,
                    beginTime,
                    beginTime,
                    endTime,
                    speed.getValue(),
                    strideLength.getValue(),
                    strideFrequency.getValue(),
                    0.0,
                    false,
                    "",
                    null
            );
        } else {
            /* TODO: Should be improved as part of #449. */
            return new FallRiskProfile();
        }
    }

    /**
     * Construct an FRP from a given set of footsteps and a custom time window. The custom time window allows one to
     * specify a time window that is used as wthe walking time for the parameters: (1) average speed and (2) average
     * step frequency.
     *
     * @return an FRP based on the given set of footsteps and a custom time window.
     */
    public static FallRiskProfile fromFootstepsAndTimeWindow(List<Footstep> footsteps, TimeWindow timeWindow) {
        List<Footstep> footstepsWithPosition =
                footsteps.stream().filter(Footstep::hasPosition).toList();
        if (!footstepsWithPosition.isEmpty()) {
            Floor floor = footstepsWithPosition.get(0).getFloor();
            Wearable wearable = footstepsWithPosition.get(0).getWearable();
            AverageSpeed speed = AverageSpeed.of(footstepsWithPosition);
            AverageStrideLength strideLength = AverageStrideLength.of(footstepsWithPosition);
            AverageStrideFrequency strideFrequency = AverageStrideFrequency.of(footstepsWithPosition);
            return new FallRiskProfile(
                    floor,
                    wearable,
                    timeWindow.getBeginTime(),
                    timeWindow.getBeginTime(),
                    timeWindow.getEndTime(),
                    speed.getValue(),
                    strideLength.getValue(),
                    strideFrequency.getValue(),
                    0.0,
                    false,
                    "",
                    null
            );
        } else {
            /* TODO: Should be improved as part of #449. */
            return new FallRiskProfile();
        }
    }

    public Long getId() {
        return id;
    }

    public Wearable getWearable() {
        return wearable;
    }

    public Floor getFloor() {
        return floor;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getNotes() {
        return notes;
    }

    public Double getWalkingSpeed() {
        return walkingSpeed;
    }

    public Double getStepLength() {
        return stepLength;
    }

    public Double getStepFrequency() {
        return stepFrequency;
    }

    public Double getRmsVerticalAcceleration() {
        return rmsVerticalAcceleration;
    }

    @Nullable
    public FallRiskProfileNote getFallRiskProfileNote() {
        return fallRiskProfileNote;
    }

    public void setFallRiskProfileNote(@Nullable FallRiskProfileNote fallRiskProfileNote) {
        this.fallRiskProfileNote = fallRiskProfileNote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallRiskProfile that = (FallRiskProfile) o;
        // Null check on id's handles the case where an FRP is instantiated from a list of footsteps, then id is null.
        return ((id == null || that.id == null) || id.equals(that.id)) &&
                creationTime.equals(that.creationTime) &&
                beginTime.equals(that.beginTime) &&
                endTime.equals(that.endTime) &&
                hidden == that.hidden &&
                walkingSpeed.equals(that.walkingSpeed) &&
                stepLength.equals(that.stepLength) &&
                stepFrequency.equals(that.stepFrequency) &&
                Objects.equals(rmsVerticalAcceleration, that.rmsVerticalAcceleration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                creationTime,
                beginTime,
                endTime,
                walkingSpeed,
                stepLength,
                stepFrequency,
                rmsVerticalAcceleration
        );
    }

    @Override
    public String toString() {
        return "FallRiskProfile{" +
                "id=" + id +
                ", wearable=" + wearable +
                ", floor=" + floor +
                ", creationTime=" + creationTime +
                ", beginTime=" + beginTime +
                ", endTime=" + endTime +
                ", hidden=" + hidden +
                ", walkingSpeed=" + walkingSpeed +
                ", stepLength=" + stepLength +
                ", stepFrequency=" + stepFrequency +
                ", rmsVerticalAcceleration=" + rmsVerticalAcceleration +
                '}';
    }
}
