package smartfloor.domain.entities.rehabilitation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.Builder;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.dto.rehabilitation.TestTrialForm;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

@Embeddable
public class TestTrial {
    @Column(name = "begin_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    @Column(name = "end_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    public TestTrial() {
    }

    public TestTrial(TestTrialForm testTrialForm) {
        this.beginTime = testTrialForm.getBeginTime();
        this.endTime = testTrialForm.getEndTime();
    }

    @Builder
    public TestTrial(LocalDateTime beginTime, LocalDateTime endTime) {
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
