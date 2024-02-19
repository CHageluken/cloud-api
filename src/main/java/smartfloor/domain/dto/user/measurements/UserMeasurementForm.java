package smartfloor.domain.dto.user.measurements;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.UserMeasurementType;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

@Validated
public final class UserMeasurementForm {

    private UserMeasurementType type;

    private Long userId;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime recordedAt;

    @NotNull
    @Min(value = 0, message = "Value must be between 0 and 28")
    @Max(value = 28, message = "Value must be between 0 and 28")
    private Double value;

    @Valid
    private Map<String, Object> details;

    public UserMeasurementForm() {
    }

    /**
     * TODO.
     */
    @Builder
    public UserMeasurementForm(
            UserMeasurementType type,
            Long userId,
            LocalDateTime recordedAt,
            Double value,
            Map<String, Object> details
    ) {
        this.type = type;
        this.userId = userId;
        this.recordedAt = recordedAt;
        this.value = value;
        this.details = details;
    }

    public UserMeasurementType getType() {
        return type;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public Double getValue() {
        return value;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
