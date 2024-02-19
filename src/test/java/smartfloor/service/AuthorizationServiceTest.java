package smartfloor.service;

import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import smartfloor.configuration.TestUser;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.UserRepository;

/**
 * This class tests the authorization service.
<<<<<<< HEAD
 * It should be noted that this authorization service test borders on an
 * integration test because it (partially) sets up
 * the Spring Security context using the Spring Security User testUser =
 * getTestUser(name) User and context. This allows
 * us to test the authorization service using users with different roles and/or
 * authorities.
=======
 * It should be noted that this authorization service test borders on an integration test because it (partially)
 * sets up the Spring Security context using the Spring Security User testUser = getTestUser(name) User and context.
 * This allows us to test the authorization service using users with different roles and/or authorities.
>>>>>>> c272faceeb81d7f83e28b70b7c6686ba573f93e7
 */
@ExtendWith(MockitoExtension.class)

@Tag("UnitTest")
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private static Stream<Arguments> userUserOperationTestParams() {
        // Given a regular user (no admin or group manager role)
        return Stream.of(
                // when regular user is requesting their own user
                Arguments.of(TestUser.FIRST_REGULAR_USER.getId(), false),
                // when regular user is requesting some other user
                Arguments.of(TestUser.SECOND_REGULAR_USER.getId(), true));
    }

    private static Stream<Arguments> adminUserUserOperationTestParams() {
        // Given an admin user
        return Stream.of(
                // when admin user is requesting their own user
                Arguments.of(TestUser.FIRST_REGULAR_USER.getId()),
                // when admin user is requesting some other user
                Arguments.of(TestUser.SECOND_REGULAR_USER.getId()));
    }

    private static Stream<Arguments> groupManagerUserUserOperationTestParams() {
        // Given a group manager user
        return Stream.of(
                // when regular user is requesting some user
                Arguments.of(TestUser.FIRST_REGULAR_USER.getId(), false),
                // when regular user is requesting their own user
                Arguments.of(TestUser.GROUP_MANAGER_USER.getId(), false),
                // when regular user is requesting some other user
                Arguments.of(TestUser.SECOND_REGULAR_USER.getId(), true));
    }

    @ParameterizedTest
    @MethodSource("userUserOperationTestParams")
    void testValidateRegularUserOperationAuthority(Long userIdToValidate, boolean shouldThrowException) {
        User testUser = getTestUser("smartfloor-test");

        if (shouldThrowException) {
            assertThrows(AccessDeniedException.class, () -> {
                authorizationService.validateUserOperationAuthority(userIdToValidate);
            });
        } else {
            assertDoesNotThrow(() -> {
                authorizationService.validateUserOperationAuthority(userIdToValidate);
            });
        }
    }

    @ParameterizedTest
    @MethodSource("adminUserUserOperationTestParams")
    void testValidateAdminUserOperationAuthority(Long userIdToValidate) {
        User testUser = getTestUser("smartfloor-test-adm");
        // we should never get an (access denied) exception since we are an admin user
        // within the tenant
        assertDoesNotThrow(() -> {
            authorizationService.validateUserOperationAuthority(userIdToValidate);
        });
    }

    @ParameterizedTest
    @MethodSource("groupManagerUserUserOperationTestParams")
    void testValidateGroupManagerUserOperationAuthority(Long userIdToValidate, boolean shouldThrowException) {
        User testUser = getTestUser("smartfloor-test-gm");
        // given: the group manager user is managing the first regular user but not the
        // second
        // note: made lenient to avoid stubbing exception that does not apply for this
        // parameterized scenario
        lenient().when(
                userRepository.isManagingUser(TestUser.GROUP_MANAGER_USER.getId(), TestUser.FIRST_REGULAR_USER.getId()))
                .thenReturn(true);

        // then
        if (shouldThrowException) {
            assertThrows(AccessDeniedException.class, () -> {
                authorizationService.validateUserOperationAuthority(userIdToValidate);
            });
        } else {
            assertDoesNotThrow(() -> {
                authorizationService.validateUserOperationAuthority(userIdToValidate);
            });
        }
    }

    @Test
    void testValidateUserGroupOperationAuthority() {
        User testUser = getTestUser("smartfloor-test");
        // given: a test group and a regular user that is not a group manager requesting
        // an operation on the group
        Long testGroupId = 1L;

        // then: the user should not be allowed to perform the operation
        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.validateGroupOperationAuthority(testGroupId);
        });
    }

    @Test
    void testValidateAdminUserGroupOperationAuthority() {
        User testUser = getTestUser("smartfloor-test-adm");
        // given: a test group and an admin user requesting an operation on the group
        Long testGroupId = 1L;

        // then: the admin should be allowed to perform the operation
        assertDoesNotThrow(() -> {
            authorizationService.validateGroupOperationAuthority(testGroupId);
        });
    }

    @Test
    void testValidateGroupManagerGroupOperationAuthority() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given: a test group and a group manager user requesting an operation on the
        // group while not managing it
        Long testGroupId = 1L;

        // then: the group manager should not be allowed to perform the operation
        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.validateGroupOperationAuthority(testGroupId);
        });

        // when: the group manager is now managing the group
        when(userRepository.isManagingGroup(TestUser.GROUP_MANAGER_USER.getId(), testGroupId)).thenReturn(true);

        // then: the user should be allowed to perform the operation
        assertDoesNotThrow(() -> {
            authorizationService.validateGroupOperationAuthority(testGroupId);
        });
    }

    @Test
    void testValidateUserWearableOperationAuthority() {
        User testUser = getTestUser("smartfloor-test");
        // given: a test wearable and a regular user requesting an operation on the
        // wearable
        String testWearableId = "test-wearable-id";

        // then: the user should not be allowed to perform the operation
        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.validateCurrentWearableOperationAuthority(testWearableId);
        });
    }

    @Test
    void testValidateAdminUserWearableOperationAuthority() {
        User testUser = getTestUser("smartfloor-test-adm");
        // given: a test wearable and an admin user requesting an operation on the
        // wearable
        String testWearableId = "test-wearable-id";

        // then: the admin should be allowed to perform the operation
        assertDoesNotThrow(() -> {
            authorizationService.validateCurrentWearableOperationAuthority(testWearableId);
        });
    }

    @Test
    void testValidateGroupManagerWearableOperationAuthority() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given: a test wearable and a group manager user requesting an operation on
        // the wearable while not managing it
        String testWearableId = "test-wearable-id";

        // then: the group manager should not be allowed to perform the operation
        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.validateCurrentWearableOperationAuthority(testWearableId);
        });

        // when: the group manager is now managing the wearable
        when(userRepository.isManagerAllowedToAccessWearable(
                TestUser.GROUP_MANAGER_USER.getId(),
                testWearableId)).thenReturn(true);

        // then: the user should be allowed to perform the operation
        assertDoesNotThrow(() -> {
            authorizationService.validateCurrentWearableOperationAuthority(testWearableId);
        });
    }

    @Test
    void testValidateUserTenantOperationAuthority() {
        User testUser = getTestUser("smartfloor-test");
        Tenant tenant = Tenant.getDefaultTenant();
        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.validateTenantOperationAuthority(tenant);
        });
    }

    @Test
    void testValidateAdminUserTenantOperationAuthority() {
        User testUser = getTestUser("smartfloor-test-adm");
        // given: the default tenant and its tenant admin

        // when: the tenant admin is requesting an operation on their own tenant
        // then: the tenant admin should be allowed to perform the operation
        assertDoesNotThrow(() -> {
            authorizationService.validateTenantOperationAuthority(Tenant.getDefaultTenant().getId());
        });

        // when: the tenant admin is requesting an operation on some other tenant
        // then: the tenant admin should not be allowed to perform the operation
        Long tenantId = Tenant.getDefaultTenant().getId() + 1L;
        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.validateTenantOperationAuthority(tenantId);
        });
    }

    @Test
    void testValidateGroupManagerTenantOperationAuthority() {
        User testUser = getTestUser("smartfloor-test-gm");
        // We want a GM to not have access even to their tenant, but for now this is
        // necessary, since a GM
        // has to check the tenant limit upon user creation.
        // TODO: VIT-1096
        // assertThrows(AccessDeniedException.class, () -> {
        // authorizationService.validateTenantOperationAuthority(Tenant.getDefaultTenant().getId());
        // });

        Long tenantId = Tenant.getDefaultTenant().getId() + 1L;
        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.validateTenantOperationAuthority(tenantId);
        });
    }

    private User getTestUser(String username) {
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());

        for (TestUser user : TestUser.values()) {
            if (user.getUsername().equals(username)) {
                User testUser = User
                        .builder()
                        .tenant(Tenant.getDefaultTenant())
                        .id(user.getId())
                        .authId(user.getUsername())
                        .build();

                List<GrantedAuthority> authorities = getAuthoritiesForUser(user);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        testUser,
                        testUser.getAuthId(),
                        authorities);

                SecurityContext securityContext = new SecurityContextImpl();
                securityContext.setAuthentication(auth);
                SecurityContextHolder.setContext(securityContext);
                return testUser;
            }
        }
        throw new UsernameNotFoundException("User not found");
    }

    private List<GrantedAuthority> getAuthoritiesForUser(TestUser user) {
        switch (user) {
            case FIRST_REGULAR_USER:
            case SECOND_REGULAR_USER:
                return List.of(Role.USER.toGrantedAuthority());
            case GROUP_MANAGER_USER:
                return List.of(Role.MANAGER.toGrantedAuthority());
            case ADMIN_USER:
                return List.of(Role.ADMIN.toGrantedAuthority());
            default:
                throw new IllegalArgumentException("User not found");
        }
    }

}
