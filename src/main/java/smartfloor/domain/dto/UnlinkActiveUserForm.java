package smartfloor.domain.dto;

/**
 * A DTO object that allows one to provide a user id of a user for which an active user-wearable link
 * should be completed (ended). This effectively "unlinks" the wearable, i.e. it has no active links anymore.
 */
public class UnlinkActiveUserForm {

    private Long userId;

    public UnlinkActiveUserForm() {
    }

    public UnlinkActiveUserForm(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "UnlinkActiveUserForm{" +
                "userId=" + userId +
                '}';
    }
}
