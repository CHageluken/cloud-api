package smartfloor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.exception.TenantNotFoundException;
import smartfloor.repository.jpa.TenantRepository;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final AuthorizationService authorizationService;

    @Autowired
    public TenantService(TenantRepository tenantRepository, AuthorizationService authorizationService) {
        this.tenantRepository = tenantRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * TODO.
     */
    public Tenant getTenantById(Long tenantId) throws TenantNotFoundException {
        authorizationService.validateTenantOperationAuthority(tenantId);

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);

        if (tenant == null) {
            throw new TenantNotFoundException(tenantId);
        }

        return tenant;
    }
}
