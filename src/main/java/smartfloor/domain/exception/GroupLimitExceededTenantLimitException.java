package smartfloor.domain.exception;

public class GroupLimitExceededTenantLimitException extends Exception {
    /**
     * TODO.
     */
    public GroupLimitExceededTenantLimitException(Integer limitRemainder) {
        super(String.format(
                "Group limit exceeds tenant limit. The highest limit allowed for this group is %d",
                limitRemainder
        ));
    }
}
