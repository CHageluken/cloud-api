package smartfloor.service;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import smartfloor.IntegrationTestBase;
import smartfloor.configuration.TestUser;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

/**
 * This test class tests the authorization service. It is similar to the AuthorizationServiceTest, but this is more of
 * an integration test as it tests the service with the full Spring context and a real database (PostgreSQL).
 * The main purpose is to validate that all authorization checks and/or assumptions on the database work as expected
 * given a properly set up set of entities.
 * It should be noted that Testcontainers support for JUnit 5 is currently limited, hence we still use JUnit 4 for
 * this test class. It is not great working with JUnit 4, but getting Testcontainers to play nice in this case was
 * harder than it should be for the scope of this issue (VIT-949). So, it's a compromise.
 */

class AuthorizationServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private WearableRepository wearableRepository;
    @Autowired
    private WearableGroupRepository wearableGroupRepository;

    @Test
    void testValidateRegularUserDeniedAccessToOtherUsers() {
        User testUser = getTestUser("smartfloor-test");
        // given
        User userToAccess = User.builder()
                .tenant(Tenant.getDefaultTenant())
                .authId(TestUser.SECOND_REGULAR_USER.getUsername())
                .build();
        userToAccess = userRepository.save(userToAccess);
        final User userTemp = userToAccess;
        // when the user tries to access another user
        // then throw an exception since a regular user is not allowed to do so
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateUserOperationAuthority(userTemp)
        );
    }

    @Test
    void testValidateRegularUserAllowedAccessToSelf() {
        User testUser = getTestUser("smartfloor-test");
        final User self = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        authorizationService.validateUserOperationAuthority(self);
    }

    /**
     * This test validates that a regular user is not allowed to access a group they are a member of.
     * This seems counter-intuitive, but it is a requirement that regular users do not view metadata about the groups
     * they are part of. This since this metadata contains information about other users in the group, which is
     * considered sensitive information.
     */
    @Test
    void testValidateRegularUserDeniedAccessToOwnGroup() {
        User testUser = getTestUser("smartfloor-test");
        // given
        User authenticatedUser = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        User userToAccess = userRepository
                .findByAuthId(TestUser.SECOND_REGULAR_USER.getUsername())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .tenant(Tenant.getDefaultTenant())
                                .authId(TestUser.SECOND_REGULAR_USER.getUsername())
                                .build()
                ));
        Group group = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-validate-regular-user-denied-access-to-own-group")
                .users(List.of(authenticatedUser, userToAccess)) // requesting user is part of the group
                .build();
        final Group tempGroup = groupRepository.save(group);
        // when the user tries to access a group they are a member of
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateGroupOperationAuthority(tempGroup)
        );
        // then deny access since they are not a manager of the group
    }

    @Test
    void testValidateRegularUserDeniedAccessToWearablesOutsideOfTheirGroup() {
        User testUser = getTestUser("smartfloor-test");
        // given
        User authenticatedUser = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        Wearable wearable = Wearable.builder()
                .id("test-validate-regular-user-denied-access-to-group-wearables")
                .build();
        WearableGroup wearableGroup = WearableGroup.builder()
                .name("test-validate-regular-user-denied-access-to-group-wearables")
                .wearables(List.of(wearable))
                .build();
        Group group1 = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-validate-regular-user-denied-access-to-group-wearables-1")
                .users(List.of(authenticatedUser))
                .build();
        Group group2 = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-validate-regular-user-denied-access-to-group-wearables-2")
                .wearableGroup(wearableGroup)
                .build();
        final Wearable tempWearable = wearableRepository.save(wearable);
        wearableGroupRepository.save(wearableGroup);
        groupRepository.save(group1);
        groupRepository.save(group2);

        // when the user tries to access a wearable from a group they are NOT a member of
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateCurrentWearableOperationAuthority(tempWearable)
        );
        // then deny access since they are not a manager of the group
    }

    @Test
    void testValidateRegularUserAllowedToGetGroupWearables() {
        User testUser = getTestUser("smartfloor-test");
        // given
        User authenticatedUser = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        Wearable wearable = Wearable.builder()
                .id("test-validate-regular-user-allowed-to-get-group-wearables")
                .build();
        WearableGroup wearableGroup = WearableGroup.builder()
                .name("test-validate-regular-user-allowed-to-get-group-wearables")
                .wearables(List.of(wearable))
                .build();
        Group group = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-validate-regular-user-allowed-to-get-group-wearables")
                .users(List.of(authenticatedUser))
                .wearableGroup(wearableGroup)
                .build();
        wearable = wearableRepository.save(wearable);
        wearableGroupRepository.save(wearableGroup);
        groupRepository.save(group);
        // when the user tries to access the wearables of a group they are a member of
        authorizationService.validateCurrentWearableOperationAuthority(wearable);
    }

    @Test
    void testRegularUserDeniedAccessToTenant() {
        User testUser = getTestUser("smartfloor-test");
        // given
        final User authenticatedUser = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        // when the user tries to access the tenant
        Tenant tenant = authenticatedUser.getTenant();
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateTenantOperationAuthority(tenant)
        );
        // then deny access since they are not a (tenant) admin
    }

    @Test
    void testValidateGroupManagerAllowedAccessToManagedGroupAndMembers() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given
        User authenticatedUser = userRepository.findByAuthId(TestUser.GROUP_MANAGER_USER.getUsername()).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        User userToAccess = userRepository
                .findByAuthId(TestUser.SECOND_REGULAR_USER.getUsername())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .tenant(Tenant.getDefaultTenant())
                                .authId(TestUser.SECOND_REGULAR_USER.getUsername())
                                .build()
                ));
        Group group = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-group-manager-allowed-access-to-managed-group-and-members")
                .users(List.of(userToAccess))
                .managers(List.of(authenticatedUser))
                .build();
        group = groupRepository.save(group);
        // when the group manager tries to access one of their managed groups
        authorizationService.validateGroupOperationAuthority(group);
        // when the group manager tries to access a user in one of their groups
        authorizationService.validateUserOperationAuthority(userToAccess);
    }

    @Test
    void testValidateGroupManagerDeniedAccessToGroupTheyDoNotManage() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given
        User groupUser = userRepository
                .findByAuthId(TestUser.SECOND_REGULAR_USER.getUsername())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .tenant(Tenant.getDefaultTenant())
                                .authId(TestUser.SECOND_REGULAR_USER.getUsername())
                                .build()
                ));
        Group unmanagedGroup = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-group-manager-denied-access-to-group-they-do-not-manage")
                .users(List.of(groupUser))
                .build();
        final Group tempUnmanagedGroup = groupRepository.save(unmanagedGroup);
        // when the group manager tries to access a user in another group
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateGroupOperationAuthority(tempUnmanagedGroup)
        );
        // then throw an exception since the group manager is not allowed to do so
    }

    @Test
    void testValidateGroupManagerDeniedAccessToGroupMembersInOtherGroups() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given
        final User userToAccess = userRepository
                .findByAuthId(TestUser.SECOND_REGULAR_USER.getUsername())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .tenant(Tenant.getDefaultTenant())
                                .authId(TestUser.SECOND_REGULAR_USER.getUsername())
                                .build()
                ));
        Group unmanagedGroup = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-group-manager-denied-access-to-group-members-in-other-groups")
                .users(List.of(userToAccess))
                .build();
        groupRepository.save(unmanagedGroup);
        // when the group manager tries to access a user in another group
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateUserOperationAuthority(userToAccess)
        );
        // then throw an exception since the group manager is not allowed to do so
    }

    @Test
    void test_Validate_GroupManager_Allowed_Access_To_Wearable_Associated_With_Managed_Group() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given
        User authenticatedUser = userRepository.findByAuthId(TestUser.GROUP_MANAGER_USER.getUsername()).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        Wearable wearable = Wearable.builder()
                .id("test-validate-group-manager-allowed-access-to-wearable-associated-with-managed-group")
                .build();
        WearableGroup wearableGroup = WearableGroup
                .builder()
                .name("test-validate-group-manager-allowed-access-to-wearable-associated-with-managed-group")
                .wearables(List.of(wearable))
                .build();
        Group group = Group
                .builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-validate-group-manager-allowed-access-to-wearable-associated-with-managed-group")
                .managers(List.of(authenticatedUser))
                .wearableGroup(wearableGroup)
                .build();
        wearable = wearableRepository.save(wearable);
        wearableGroupRepository.save(wearableGroup);
        groupRepository.save(group);
        // when the group manager tries to access the wearables of a group they manage
        authorizationService.validateCurrentWearableOperationAuthority(wearable);
        // then allow access since they are a manager of the group
    }

    @Test
    void test_Validate_GroupManager_Denied_Access_To_Wearable_Associated_With_Unmanaged_Group() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given
        Wearable wearable = Wearable.builder()
                .id("test-validate-group-manager-denied-access-to-wearable-associated-with-unmanaged-group")
                .build();
        WearableGroup wearableGroup = WearableGroup.builder()
                .name("test-validate-group-manager-denied-access-to-wearable-associated-with-unmanaged-group")
                .wearables(List.of(wearable))
                .build();
        Group group = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-validate-group-manager-denied-access-to-wearable-associated-with-unmanaged-group")
                .wearableGroup(wearableGroup)
                .build();
        final Wearable tempWearable = wearableRepository.save(wearable);
        wearableGroupRepository.save(wearableGroup);
        groupRepository.save(group);
        // when the group manager tries to access the wearables of a group they do not manage
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateCurrentWearableOperationAuthority(tempWearable)
        );
        // then deny access since they are not a manager of the group
    }

    @Test
    void test_Validate_GroupManager_Denied_Access_To_Own_Tenant() {
        User testUser = getTestUser("smartfloor-test-gm");
        // given
        User authenticatedUser = userRepository.findByAuthId(TestUser.GROUP_MANAGER_USER.getUsername()).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        // when the group manager tries to access the tenant
        // TODO: VIT-1096
        //        authorizationService.validateTenantOperationAuthority(authenticatedUser.getTenant());
        // then deny access since they are not a (tenant) admin
        final Long tempId = authenticatedUser.getTenant().getId() + 1L;
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.validateTenantOperationAuthority(tempId)
        );
    }

    @Test

    void test_Validate_Admin_Allowed_Access_To_User() {
        User testUser = getTestUser("smartfloor-test-adm");
        // given
        User user = User.builder()
                .tenant(Tenant.getDefaultTenant())
                .authId("test-admin-allowed-access-to-user")
                .build();
        user = userRepository.save(user);
        // when the tenant admin tries to access the user
        authorizationService.validateUserOperationAuthority(user);
        // then allow access since they are a (tenant) admin
    }

    @Test
    void test_Validate_Admin_Allowed_Access_To_Group() {
        User testUser = getTestUser("smartfloor-test-adm");
        // given
        Group group = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test-admin-allowed-access-to-group")
                .build();
        group = groupRepository.save(group);
        // when the tenant admin tries to access the group
        authorizationService.validateGroupOperationAuthority(group);
        // then allow access since they are a (tenant) admin
    }

    @Test
    void test_Validate_Admin_Allowed_Access_To_Wearable() {
        User testUser = getTestUser("smartfloor-test-adm");
        // given
        Wearable wearable = Wearable.builder()
                .id("test-admin-allowed-access-to-wearable")
                .build();
        wearable = wearableRepository.save(wearable);
        // when the tenant admin tries to access the wearable
        authorizationService.validateCurrentWearableOperationAuthority(wearable);
        // then allow access since they are a (tenant) admin
    }

    @Test
    void test_Validate_Admin_Allowed_Access_To_Tenant() {
        User testUser = getTestUser("smartfloor-test-adm");
        // given
        User authenticatedUser = userRepository.findByAuthId(TestUser.ADMIN_USER.getUsername()).orElseThrow(
                () -> new RuntimeException("Test user not found")
        );
        // when the tenant admin tries to access the tenant
        authorizationService.validateTenantOperationAuthority(authenticatedUser.getTenant());
        // then allow access since they are a (tenant) admin
    }

    private User getTestUser(String username) {
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());

        for (TestUser user : TestUser.values()) {
            if (user.getUsername().equals(username)) {
                User testUser = userRepository
                        .findByAuthId(user.getUsername())
                        .orElseGet(() -> userRepository.save(
                                User.builder()
                                        .tenant(Tenant.getDefaultTenant())
                                        .authId(user.getUsername())
                                        .build()
                        ));

                List<GrantedAuthority> authorities = getAuthoritiesForUser(user);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        testUser,
                        testUser.getAuthId(),
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

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
