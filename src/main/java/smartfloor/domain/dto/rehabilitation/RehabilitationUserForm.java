package smartfloor.domain.dto.rehabilitation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * Wrapper object for creating a rehabilitation user. A rehabilitation user is (currently) mainly useful for the
 * rehabilitation research use case: we need a device user (a user that is wearing a device but does not login).
 * The device user is linked to one or more wearables for the given time window [beginTime, endTime].
 * The device user is assigned to a (rehabilitation) group with the given group id.
 */
public class RehabilitationUserForm {

    private String leftWearableId;
    private String rightWearableId;
    private Long groupId;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    public RehabilitationUserForm() {
    }

    /**
     * TODO.
     */
    public RehabilitationUserForm(
            String leftWearableId,
            String rightWearableId,
            Long groupId,
            LocalDateTime beginTime,
            LocalDateTime endTime
    ) {
        this.leftWearableId = leftWearableId;
        this.rightWearableId = rightWearableId;
        this.groupId = groupId;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public String getLeftWearableId() {
        return leftWearableId;
    }

    public void setLeftWearableId(String leftWearableId) {
        this.leftWearableId = leftWearableId;
    }

    public String getRightWearableId() {
        return rightWearableId;
    }

    public void setRightWearableId(String rightWearableId) {
        this.rightWearableId = rightWearableId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
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

}
