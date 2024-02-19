package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.dto.UserLimit;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.util.TestUtils;

class TenantControllerIntegrationTest extends IntegrationTestBase {

    private static final String TENANT_USER_LIMIT_REST_ENDPOINT = "/tenants/user-limit";
    private static final int DEFAULT_USER_LIMIT = 1;
    /**
     * We have to hardcode the tenant id of the "limited" tenant since we are not allowed to look up and/or create
     * tenants. This is because the row-level security policies also apply to the tenants table, which restricts us to
     * operations within our current tenant context.
     */
    private static final long TENANT_WITH_USER_LIMIT_TENANT_ID = 3L;
    @Autowired
    UserRepository userRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private User authenticatedUserWithLimit;

    private Tenant getTenantWithUserLimit() {
        Tenant tenantWithLimit = Tenant
                .builder()
                .name("limited")
                .userLimit(DEFAULT_USER_LIMIT)
                .build();

        return tenantRepository
                .findByName(tenantWithLimit.getName())
                .orElseGet(() -> tenantRepository.save(tenantWithLimit));
    }


    /**
     * Create the test users for the tenants relevant to this test:
     * <p>1. The default tenant - used throughout the entire test suite.
     * 2. The user "limited" tenant - a tenant created especially for this test, this tenant has a user limit set. Note
     * that this tenant is provisioned through migrations as we are unable to create it during this test. This is
     * because we are limited by the row-level security policies setup for multitenancy (since they also apply to the
     * test suite).</p>
     * To be allowed (by the row-level security policies) to create the users, we should first fix our tenant context
     * to that of the relevant tenants. For more information about the row-level security policies,
     * view their migration.
     */
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        getTestUser();

        AccessScopeContext.INSTANCE.setTenantId(TENANT_WITH_USER_LIMIT_TENANT_ID);
        userRepository.deleteAll();
        User limitedAuthenticatedUser = User
                .builder()
                .authId(TestUtils.TEST_USER_AUTH_ID)
                .tenant(getTenantWithUserLimit())
                .build();
        authenticatedUserWithLimit = userRepository.save(limitedAuthenticatedUser);
    }

    @Test
    void testGetAccessibleUserLimitForTenantWithoutLimit() throws JsonProcessingException {
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(TENANT_USER_LIMIT_REST_ENDPOINT, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        UserLimit userLimit = mapper.readValue(response.getBody(), new TypeReference<>() {
        });

        // then
        assertNull(userLimit.getValue());
    }

    @Test
    void testGetAccessibleUserLimitForTenantWithLimit() throws JsonProcessingException {
        // given
        AccessScopeContext.INSTANCE.setTenantId(TENANT_WITH_USER_LIMIT_TENANT_ID);

        // when

        // use limited tenant's authenticated user (which manages the group) to (attempt to) create the users
        HttpHeaders headers = TestUtils.withAuthenticatedUserHttpHeaders(authenticatedUserWithLimit);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(TENANT_USER_LIMIT_REST_ENDPOINT, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        UserLimit userLimit = mapper.readValue(response.getBody(), new TypeReference<>() {
        });

        // then
        assertNotNull(userLimit.getValue());
        assertEquals(Optional.of(userLimit.getValue()), Optional.of(DEFAULT_USER_LIMIT));
    }
}
