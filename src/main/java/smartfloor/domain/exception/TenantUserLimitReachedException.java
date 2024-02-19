package smartfloor.domain.exception;

public class TenantUserLimitReachedException extends Exception {
    /**
     * TODO.
     */
    public TenantUserLimitReachedException(Integer userLimit) {
        super(String.format(
                "User limit reached for current tenant. The amount of users allowed for this tenant is %d",
                userLimit
        ));
    }
}
