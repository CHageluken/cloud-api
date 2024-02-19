package smartfloor.multitenancy;

import smartfloor.domain.UserType;

/**
 * <p>This singleton is used to store the current user type and either a tenant id or a composite user id in a thread
 * local variable. This provides an access "context" that the rest of the application can use to store and determine the
 * tenant or composite user (id) for which the current thread is operating.</p>
 * Normally, initially determining the values of those variables is facilitated for a thread through an incoming web
 * request. See filter.AccessScopeContextFilter for more information.
 */
public enum AccessScopeContext {
    INSTANCE;

    private final ThreadLocal<UserType> userType = new ThreadLocal<>();
    private final ThreadLocal<Long> tenantId = new ThreadLocal<>();
    private final ThreadLocal<Long> compositeUserId = new ThreadLocal<>();

    public Long getTenantId() {
        return tenantId.get();
    }

    public void setTenantId(Long tenantId) {
        this.tenantId.set(tenantId);
    }

    public Long getCompositeUserId() {
        return compositeUserId.get();
    }

    public void setCompositeUserId(Long compositeUserId) {
        this.compositeUserId.set(compositeUserId);
    }

    public UserType getUserType() {
        return userType.get();
    }

    public void setUserType(UserType userType) {
        this.userType.set(userType);
    }
}
