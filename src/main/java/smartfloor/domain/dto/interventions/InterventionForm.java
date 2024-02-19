package smartfloor.domain.dto.interventions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import lombok.Builder;
import org.springframework.lang.Nullable;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.interventions.FallPreventionProgram;
import smartfloor.domain.entities.interventions.InterventionType;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * DTO for creating a single Intervention.
 */
public class InterventionForm {

    private InterventionType interventionType;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Nullable
    private LocalDateTime endTime;
    @Nullable
    private FallPreventionProgram fallPreventionProgram;
    @Nullable
    private String otherProgram;

    /**
     * An intervention form is used for the creation of a single Intervention.
     */
    @Builder
    public InterventionForm(
            InterventionType interventionType,
            LocalDateTime beginTime,
            @Nullable LocalDateTime endTime,
            @Nullable FallPreventionProgram fallPreventionProgram,
            @Nullable String otherProgram
    ) {
        this.interventionType = interventionType;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.fallPreventionProgram = fallPreventionProgram;
        this.otherProgram = otherProgram;
    }

    public InterventionForm() {

    }


    public InterventionType getInterventionType() {
        return interventionType;
    }

    public void setInterventionType(InterventionType interventionType) {
        this.interventionType = interventionType;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
    }

    @Nullable
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(@Nullable LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Nullable
    public FallPreventionProgram getFallPreventionProgram() {
        return fallPreventionProgram;
    }

    public void setFallPreventionProgram(@Nullable FallPreventionProgram fallPreventionProgram) {
        this.fallPreventionProgram = fallPreventionProgram;
    }

    @Nullable
    public String getOtherProgram() {
        return otherProgram;
    }

    public void setOtherProgram(@Nullable String otherProgram) {
        this.otherProgram = otherProgram;
    }
}
