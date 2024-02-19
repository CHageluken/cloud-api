package smartfloor.domain.dto;

import lombok.Builder;
import org.springframework.lang.Nullable;
import smartfloor.domain.entities.UserInfo;

public class CreateWearableUserForm {
    /**
     * The id of the group that the user is added to.
     */
    private Long userGroupId;

    @Nullable
    private UserInfo info;

    public CreateWearableUserForm() {
    }

    @Builder
    public CreateWearableUserForm(Long userGroupId, UserInfo info) {
        this.userGroupId = userGroupId;
        this.info = info;
    }

    public Long getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(Long userGroupId) {
        this.userGroupId = userGroupId;
    }

    @Nullable
    public UserInfo getInfo() {
        return info;
    }

    public void setInfo(@Nullable UserInfo info) {
        this.info = info;
    }
}
