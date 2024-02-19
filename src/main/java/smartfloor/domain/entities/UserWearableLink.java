package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Builder;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.dto.UserWearableLinkForm;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * This entity represents the link between a user and a wearable within a certain timeframe.
 * It represents an association of the user with the wearable during this timeframe, namely the user is said to be
 * wearing this wearable on some side of their body.
 */
@Entity
@Table(name = "user_wearable_links")
public class UserWearableLink implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * References the user that is wearing the wearable during this session.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * References the wearable that is worn by the user during this session.
     */
    @ManyToOne
    @JoinColumn(name = "wearable_id", nullable = false)
    private Wearable wearable;

    /**
     * Begin and end time, define the timeframe in which the link or association between the user and the
     * wearable holds.
     */
    @Column(name = "begin_time", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime beginTime;

    @Column(name = "end_time")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    @Column(name = "side")
    private Wearable.Side side;

    public UserWearableLink() {
    }

    /**
     * TODO.
     */
    @Builder
    public UserWearableLink(
            User user,
            Wearable wearable,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            Wearable.Side side
    ) {
        this.user = user;
        this.wearable = wearable;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.side = side;
    }

    /**
     * <p>Construct user wearable link from a DTO object UserWearableLinkForm.</p>
     * #446: At least for now, we will not support letting the time range in which the UWL holds to be determined by
     * the client. We will instead make the interval for a client provided UWL be (now, null) => i.e. a link starting
     * from the current time that stays valid until it is completed/ended.
     *
     * @param userWearableLinkForm a DTO object that describes the user-wearable link that should be created
     */
    public UserWearableLink(UserWearableLinkForm userWearableLinkForm, User user, Wearable wearable) {
        this.user = user;
        this.wearable = wearable;
        this.beginTime = LocalDateTime.now();
        this.endTime = LocalDateTime.of(9999, 12, 31, 23, 59);
        this.side = userWearableLinkForm.getSide() != null ? userWearableLinkForm.getSide() : Wearable.Side.RIGHT;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Wearable getWearable() {
        return wearable;
    }

    public void setWearable(Wearable wearable) {
        this.wearable = wearable;
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

    @JsonIgnore
    public boolean isActive() {
        return endTime.isAfter(LocalDateTime.now());
    }

    /**
     * Method that "completes" or "ends" the user-wearable link. It sets its end time to the current time to indicate
     * that the user-wearable link no longer holds beyond this point in time.
     */
    public void complete() {
        this.endTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "UserWearableLink{" +
                "id=" + id +
                ", user=" + user +
                ", wearableId=" + wearable.getId() +
                ", side=" + side +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserWearableLink that = (UserWearableLink) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(wearable, that.wearable) &&
                Objects.equals(beginTime, that.beginTime) &&
                Objects.equals(endTime, that.endTime) &&
                side == that.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, wearable, beginTime, endTime, side);
    }
}
