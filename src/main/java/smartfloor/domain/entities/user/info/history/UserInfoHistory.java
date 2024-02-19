package smartfloor.domain.entities.user.info.history;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.UserInfo;
import smartfloor.serializer.CustomLocalDateTimeSerializer;


/**
 * Represents the history of a user's info. This is used to store the user's info up until a certain point in time.
 * Note: we currently do not actively use the history at the application-level, as it is not offered as functionality.
 * We therefore do not define a JPA relationship between the User and UserInfoHistory entity for now.
 */
@Entity
@IdClass(UserInfoHistoryId.class)
@Table(name = "user_info_history")
public class UserInfoHistory {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "end_time")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    @Column(name = "gender")
    private String gender;

    @Column(name = "admission_diagnosis")
    private String admissionDiagnosis;

    @Column(name = "secondary_diagnosis")
    private String secondaryDiagnosis;

    @Column(name = "relevant_medication")
    private String relevantMedication;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "age")
    private Integer age;

    @Column(name = "orthosis")
    private String orthosis;

    @Column(name = "shoes")
    private String shoes;

    @Column(name = "walking_aid")
    private String walkingAid;

    protected UserInfoHistory() {
    }

    /**
     * Creates a new UserInfo object from the values in this UserInfoHistory object.
     * Useful for testing.
     *
     * @return a new UserInfo object with the values from this UserInfoHistory object
     */
    public UserInfo getInfo() {
        return UserInfo.builder()
                .gender(gender)
                .admissionDiagnosis(admissionDiagnosis)
                .secondaryDiagnosis(secondaryDiagnosis)
                .relevantMedication(relevantMedication)
                .height(height)
                .weight(weight)
                .age(age)
                .orthosis(orthosis)
                .shoes(shoes)
                .walkingAid(walkingAid)
                .build();
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}