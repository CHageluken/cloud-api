package smartfloor.domain.dto.rehabilitation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

public class TestResultForm {

    private TestType type;

    private Long userId;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    private List<TestTrialForm> trials;

    private List<TestSurveyForm> surveys;

    private String remarks;

    @Nullable
    private WearableForm wearableWithSide;

    public TestResultForm() {
    }

    /**
     * TODO.
     */
    @Builder
    public TestResultForm(
            TestType type,
            Long userId,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            List<TestTrialForm> trials,
            List<TestSurveyForm> surveys,
            String remarks,
            WearableForm wearable
    ) {
        this.type = type;
        this.userId = userId;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.trials = trials;
        this.surveys = surveys;
        this.remarks = remarks;
        this.wearableWithSide = wearable;
    }

    public TestType getType() {
        return type;
    }

    public void setType(TestType type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public List<TestTrialForm> getTrials() {
        return trials;
    }

    public void setTrials(List<TestTrialForm> trials) {
        this.trials = trials;
    }

    public List<TestSurveyForm> getSurveys() {
        return surveys;
    }

    public void setSurveys(List<TestSurveyForm> surveys) {
        this.surveys = surveys;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Nullable
    public WearableForm getWearable() {
        return wearableWithSide;
    }

    public void setWearable(@Nullable WearableForm wearable) {
        this.wearableWithSide = wearable;
    }
}
