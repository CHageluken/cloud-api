package smartfloor.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.Wearable;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * Wrapper object for representing a manually constructed user-wearable association within a given timeframe (upon
 * session creation for example).
 */
public class UserWearableLinkForm {

    private Long userId;
    private String wearableId;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    private Wearable.Side side;

    public UserWearableLinkForm() {
    }

    /**
     * TODO.
     */
    public UserWearableLinkForm(
            Long userId,
            String wearableId,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            Wearable.Side side
    ) {
        this.userId = userId;
        this.wearableId = wearableId;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.side = side;
    }

    /**
     * TODO.
     */
    public UserWearableLinkForm(Long userId, String wearableId) {
        this.userId = userId;
        this.wearableId = wearableId;
        this.beginTime = LocalDateTime.now(); // default begin time is current time if not specified
        this.endTime = LocalDateTime.of(9999, 12, 31, 23, 59); // default end time if not specified
        this.side = Wearable.Side.RIGHT; // default side is right if not specified, see web-vitality#339.
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getWearableId() {
        return wearableId;
    }

    public void setWearableId(String wearableId) {
        this.wearableId = wearableId;
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

    public Wearable.Side getSide() {
        return side;
    }

    public void setSide(Wearable.Side side) {
        this.side = side;
    }

    @Override
    public String toString() {
        return "UserWearableLinkForm{" +
                "userId='" + userId + '\'' +
                ", wearableId='" + wearableId + '\'' +
                ", side=" + side +
                '}';
    }
}
