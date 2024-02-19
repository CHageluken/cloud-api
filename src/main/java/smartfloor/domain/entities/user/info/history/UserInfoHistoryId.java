package smartfloor.domain.entities.user.info.history;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Defines a composite primary key (userId, endTime) for user info history.
 * Note: as of VIT-647 we've wanted to add a surrogate key 'id' to the UserInfoHistory entity. But the table was
 * already populated in production, so we've decided against it.
 */
public class UserInfoHistoryId implements Serializable {
    private Long userId;
    private LocalDateTime endTime;

    public UserInfoHistoryId() {
    }

    public UserInfoHistoryId(Long userId, LocalDateTime endTime) {
        this.userId = userId;
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfoHistoryId that = (UserInfoHistoryId) o;
        return userId.equals(that.userId) && endTime.equals(that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, endTime);
    }
}
