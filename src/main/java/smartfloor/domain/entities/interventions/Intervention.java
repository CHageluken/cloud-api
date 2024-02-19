package smartfloor.domain.entities.interventions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Builder;
import smartfloor.converters.InterventionFallPreventionProgramAttributeConverter;
import smartfloor.converters.InterventionTypeAttributeConverter;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.User;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * We want to learn how various interventions methods affect the fall risk of clients. This entity allows for such
 * analysis.
 */
@Table(name = "interventions")
@Entity
public class Intervention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "intervention_type_id", nullable = false)
    @Convert(converter = InterventionTypeAttributeConverter.class)
    private InterventionType type;

    @Column(name = "begin_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    @Column(name = "end_time")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    @Column
    private boolean deleted;

    @Column(name = "fall_prevention_program_id")
    @Convert(converter = InterventionFallPreventionProgramAttributeConverter.class)
    private FallPreventionProgram fallPreventionProgram;

    @Column(name = "other_program")
    private String otherProgram;


    public Intervention() {
    }

    /**
     * Creates an Intervention with mandatory begin time, type and user.
     */
    @Builder
    public Intervention(
            Long id,
            User user,
            InterventionType type,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            boolean deleted,
            FallPreventionProgram fallPreventionProgram,
            String otherProgram
    ) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.deleted = deleted;
        this.fallPreventionProgram = fallPreventionProgram;
        this.otherProgram = otherProgram;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public InterventionType getType() {
        return type;
    }

    public void setType(InterventionType type) {
        this.type = type;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public FallPreventionProgram getFallPreventionProgram() {
        return fallPreventionProgram;
    }

    public void setFallPreventionProgram(FallPreventionProgram fallPreventionProgram) {
        this.fallPreventionProgram = fallPreventionProgram;
    }

    public String getOtherProgram() {
        return otherProgram;
    }

    public void setOtherProgram(String otherProgram) {
        this.otherProgram = otherProgram;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Intervention)) return false;
        Intervention that = (Intervention) o;
        return deleted == that.deleted && Objects.equals(id, that.id) &&
                Objects.equals(user, that.user) && type == that.type &&
                Objects.equals(beginTime, that.beginTime) && Objects.equals(endTime, that.endTime) &&
                fallPreventionProgram == that.fallPreventionProgram &&
                Objects.equals(otherProgram, that.otherProgram);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, type, beginTime, endTime, deleted, fallPreventionProgram, otherProgram);
    }

    @Override
    public String toString() {
        return "Intervention{" +
                "id=" + id +
                ", user=" + user +
                ", type=" + type +
                ", beginTime=" + beginTime +
                ", endTime=" + endTime +
                ", deleted=" + deleted +
                ", fallPreventionProgram=" + fallPreventionProgram +
                ", otherProgram='" + otherProgram + '\'' +
                '}';
    }
}
