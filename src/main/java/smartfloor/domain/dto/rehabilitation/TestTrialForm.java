package smartfloor.domain.dto.rehabilitation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import lombok.Builder;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

public class TestTrialForm {
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    public TestTrialForm() {
    }

    @Builder
    public TestTrialForm(LocalDateTime beginTime, LocalDateTime endTime) {
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    void setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
