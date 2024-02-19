package smartfloor.domain.dto;

import lombok.Builder;
import smartfloor.domain.entities.UserInfo;

public class UpdateWearableUserForm {

    private UserInfo info;

    private Long compositeUserId;

    public UpdateWearableUserForm() {
    }

    @Builder
    public UpdateWearableUserForm(UserInfo info, Long compositeUserId) {
        this.info = info;
        this.compositeUserId = compositeUserId;
    }

    public UserInfo getInfo() {
        return info;
    }

    public void setInfo(UserInfo info) {
        this.info = info;
    }

    public Long getCompositeUserId() {
        return compositeUserId;
    }

    public void setCompositeUserId(Long compositeUserId) {
        this.compositeUserId = compositeUserId;
    }
}


