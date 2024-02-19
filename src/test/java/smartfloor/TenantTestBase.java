package smartfloor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.TenantRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.util.TestUtils;

@SpringBootTest
@ActiveProfiles("test")
@Tag("IntegrationTest")
@Testcontainers
public class TenantTestBase {

    @Autowired
    public TenantRepository tenantRepository;

    @Autowired
    public UserRepository userRepository;

    public Tenant getTestTenant() {
        return tenantRepository.findByName(Tenant.DEFAULT_TENANT_NAME)
                .orElseGet(() -> tenantRepository.save(Tenant.getDefaultTenant()));
    }

    public User getTestUser() {
        return userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID)
                .orElseGet(() -> userRepository.save(TestUtils.testUser()));
    }

    /**
     * TODO.
     */
    @BeforeEach
    public void setDefaultTenantUser() {
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        getTestUser();
    }
}
