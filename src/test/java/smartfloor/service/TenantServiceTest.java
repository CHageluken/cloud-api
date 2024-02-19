package smartfloor.service;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.exception.TenantNotFoundException;
import smartfloor.repository.jpa.TenantRepository;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuthorizationService authorizationService;

    /**
     * Service to test.
     */
    @InjectMocks
    private TenantService tenantService;

    @Test
    void testGetTenantById() throws TenantNotFoundException {
        // given
        Tenant tenant = Tenant.getDefaultTenant();
        // when
        Mockito.when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        Tenant actual = tenantService.getTenantById(tenant.getId());
        // then
        assertNotNull(actual);
        assertEquals(tenant, actual);
        Mockito.verify(authorizationService, atLeast(1)).validateTenantOperationAuthority(tenant.getId());
    }
}
