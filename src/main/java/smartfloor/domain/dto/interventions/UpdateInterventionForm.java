package smartfloor.domain.dto.interventions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import lombok.Builder;
import org.springframework.lang.Nullable;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * A form which allows updating the end time of an intervention.
 */
public class UpdateInterventionForm {
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @Nullable
    private LocalDateTime endTime;

    @Builder
    public UpdateInterventionForm(@Nullable LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public UpdateInterventionForm() {

    }

    @Nullable
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(@Nullable LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
