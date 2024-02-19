package smartfloor.domain.entities.rehabilitation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.hibernate.annotations.Type;
import org.springframework.lang.Nullable;
import smartfloor.converters.TestTypeAttributeConverter;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.dto.rehabilitation.TestResultForm;
import smartfloor.domain.entities.User;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

@Table(name = "test_results")
@Entity
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    @Convert(converter = TestTypeAttributeConverter.class)
    private TestType type;

    /**
     * References the user that is wearing the wearable(s) during this sessions.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "begin_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    @Column(name = "end_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "test_trials", joinColumns = @JoinColumn(name = "test_result_id"))
    private List<TestTrial> trials;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "test_surveys", joinColumns = @JoinColumn(name = "test_result_id"))
    private List<TestSurvey> surveys;

    @Column(name = "remarks")
    private String remarks;

    @Type(JsonBinaryType.class)
    @Column(name = "additional_info")
    private Map<String, Object> additionalInfo;

    @Column(name = "deleted")
    private boolean deleted;

    @Embedded
    @Nullable
    @AttributeOverrides(
            {
                    @AttributeOverride(name = "wearable.id", column = @Column(name = "wearable_id")),
                    @AttributeOverride(name = "wearableSide", column = @Column(name = "wearable_side")),
            }
    )
    @JsonProperty("wearable")
    private WearableWithSide wearableWithSide;

    public TestResult() {
    }

    /**
     * TODO.
     */
    @Builder
    private TestResult(
            Long id,
            TestType type,
            User user,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            List<TestTrial> trials,
            List<TestSurvey> surveys,
            String remarks,
            Map<String, Object> additionalInfo,
            WearableWithSide wearableWithSide
    ) {
        this.id = id;
        this.type = type;
        this.user = user;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.trials = trials;
        this.surveys = surveys;
        this.remarks = remarks;
        this.additionalInfo = additionalInfo;
        if (wearableWithSide != null) {
            this.wearableWithSide = wearableWithSide;
        }
        this.deleted = false;
    }

    /**
     * TODO.
     */
    public TestResult(TestResultForm testResultForm) {
        this.type = testResultForm.getType();
        this.beginTime = testResultForm.getBeginTime();
        this.endTime = testResultForm.getEndTime();
        this.remarks = testResultForm.getRemarks();
        this.deleted = false;
    }

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public TestType getType() {
        return type;
    }

    public void setType(TestType type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public List<TestTrial> getTrials() {
        return trials;
    }

    public void setTrials(List<TestTrial> trials) {
        this.trials = trials;
    }

    public List<TestSurvey> getSurveys() {
        return surveys;
    }

    public void setSurveys(List<TestSurvey> surveys) {
        this.surveys = surveys;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Nullable
    public WearableWithSide getWearableWithSide() {
        return wearableWithSide;
    }

    public void setWearableWithSide(@Nullable WearableWithSide wearableWithSide) {
        this.wearableWithSide = wearableWithSide;
    }

    /**
     * Update (static) details based on provided test result DTO.
     */
    public void updateDetails(TestResultForm testResultForm) {
        this.setType(testResultForm.getType());
        this.setBeginTime(testResultForm.getBeginTime());
        this.setEndTime(testResultForm.getEndTime());
        this.setRemarks(testResultForm.getRemarks());
        this.setTrials(testResultForm.getTrials().stream().map(TestTrial::new).toList());
        this.setSurveys(testResultForm.getSurveys().stream().map(TestSurvey::new).toList());
    }
}
