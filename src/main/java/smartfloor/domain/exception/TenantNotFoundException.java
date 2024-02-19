package smartfloor.domain.exception;

public class TenantNotFoundException extends Exception {
    public TenantNotFoundException(String message) {
        super("Tenant with the provided identifier does not exist.");
    }

    public TenantNotFoundException(Long tenantId) {
        super(String.format("Tenant with id %d does not exist.", tenantId));
    }
}
