package smartfloor.domain.dto;

import lombok.Builder;

public class UserLimit {
    private UserLimit() {
    }

    Integer value;

    @Builder
    public UserLimit(Integer userLimit) {
        this.value = userLimit;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
