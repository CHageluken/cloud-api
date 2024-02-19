package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.domain.dto.UserLimit;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.util.TestUtils;

class GroupControllerIntegrationTest extends IntegrationTestBase {

    private static final String GROUPS_REST_ENDPOINT = "/groups";
    private static final String USER_LIMIT_REST_ENDPOINT = "/user-limit";
    private static final long TENANT_WITH_USER_LIMIT_TENANT_ID = 3L;
    private static final int DEFAULT_USER_LIMIT = 1;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private CompositeUserRepository compositeUserRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    private User authenticatedUser;
    private User authenticatedUserWithLimit;

    private Tenant getTestTenantWithUserLimit() {
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
     * TODO.
     */
    @BeforeEach
    void setUp() {
        groupRepository.deleteAll();
        userRepository.deleteAll();
        authenticatedUser = getTestUser();

        AccessScopeContext.INSTANCE.setTenantId(TENANT_WITH_USER_LIMIT_TENANT_ID);
        groupRepository.deleteAll();
        userRepository.deleteAll();
        User limitedAuthenticatedUser = User
                .builder()
                .authId(TestUtils.TEST_USER_AUTH_ID)
                .tenant(getTestTenantWithUserLimit())
                .build();
        authenticatedUserWithLimit = userRepository.save(limitedAuthenticatedUser);

        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId()); // set default tenant context again
    }

    private List<User> getGroupUsers() {
        User firstUser = User.builder().tenant(getTestTenant()).authId("first").build();
        User secondUser = User.builder().tenant(getTestTenant()).authId("second").build();
        User inaccessibleUser = User.builder().tenant(getTestTenant()).authId("third").build();

        return List.of(firstUser, secondUser, inaccessibleUser);
    }

    /**
     * <p>This test will test that a list of groups can be obtained for a regular user, with each group containing only
     * the fields that are allowed for a regular user to view (by asserting on the complement).</p>
     * The test also makes sure that a composite user (CU) can view the groups their sub-users are members of.
     *
     * @throws IOException if the response body cannot be parsed
     */
    @Test
    void testGetGroupsForUser() throws IOException {
        // given: Two groups - one containing the auth user, the other not.
        assertTrue(groupRepository.findAll().isEmpty());
        List<User> users = getGroupUsers();
        Group accessibleGroup = Group.builder()
                .tenant(getTestTenant())
                .name("accessibleGroup")
                .managers(List.of(users.get(0)))
                .users(List.of(authenticatedUser))
                .build();

        Group inaccessibleGroup = Group.builder()
                .tenant(getTestTenant())
                .name("inaccessibleGroup")
                .managers(List.of(users.get(1)))
                .users(List.of(users.get(2)))
                .build();

        userRepository.saveAll(users);
        groupRepository.saveAll(List.of(accessibleGroup, inaccessibleGroup));
        // when: Get groups for direct user with the USER role
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeadersWithRole(Role.USER));
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<Group> groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then: The response should contain 1 group - the accessible one.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, groups.size());
        Group returnedGroup = groups.get(0);
        assertEquals(accessibleGroup.getId(), returnedGroup.getId());
        // assert the group contains no fields that are not allowed for this regular user (manager/admin-only fields)
        assertNull(returnedGroup.getUsers());
        assertNull(returnedGroup.getManagers());
        assertNull(returnedGroup.getThingGroupName());
        assertNull(returnedGroup.getWearableGroup());
        assertNull(returnedGroup.getUserLimit());

        // then when: Test the same endpoint, but authenticated as a CU, who has the test user as a sub-user.
        HttpHeaders compositeUserHeaders1 = setCUWithUserAndGetHTTPHeaders(
                authenticatedUser,
                authenticatedUser.getAuthId() + "CU"
        );
        entity = new HttpEntity<>(null, compositeUserHeaders1);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then: The CU has access to their sub-user's group, so we expect the response to contain it.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, groups.size());
        returnedGroup = groups.get(0);
        assertEquals(accessibleGroup.getId(), returnedGroup.getId());

        // then when: Test the same endpoint, this time authenticated as a CU with no sub-users.
        HttpHeaders compositeUserHeaders2 = setCUWithUserAndGetHTTPHeaders(
                null,
                "testGetGroupsForCU"
        );
        entity = new HttpEntity<>(null, compositeUserHeaders2);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then: This CU has no sub-users, so they do not have access to any groups. The response must be an empty list.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, groups.size());
    }

    /**
     * This test will test that a list of groups can be obtained for a manager, with each group containing only
     * the fields that are allowed for a manager to view (by asserting on the complement).
     *
     * @throws IOException if the response body cannot be parsed
     */
    @Test
    void testGetGroupsForManager() throws IOException {
        // given
        assertTrue(groupRepository.findAll().isEmpty());
        List<User> users = getGroupUsers();
        Group group = Group.builder()
                .tenant(getTestTenant())
                .name("test")
                .managers(List.of(authenticatedUser))
                .users(users)
                .build();
        userRepository.saveAll(users);
        groupRepository.save(group);
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeadersWithRole(Role.MANAGER));
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<Group> groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Group returnedGroup = groups.get(0);
        assertEquals(group.getId(), returnedGroup.getId());
        // assert that the group contains no fields that are not allowed for this manager (admin-only fields)
        assertNull(returnedGroup.getManagers());

    }

    /**
     * This test will test that a list of groups can be obtained for an admin, with each group containing all fields
     * (by asserting on the fields that are admin-only to not be null).
     *
     * @throws IOException if the response body cannot be parsed
     */
    @Test
    void testGetGroupsForAdmin() throws IOException {
        // given
        assertTrue(groupRepository.findAll().isEmpty());
        List<User> users = getGroupUsers();
        Group group = Group.builder()
                .tenant(getTestTenant())
                .name("test")
                .managers(List.of(authenticatedUser))
                .users(users)
                .build();
        userRepository.saveAll(users);
        groupRepository.save(group);
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeadersWithRole(Role.ADMIN));
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<Group> groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Group returnedGroup = groups.get(0);
        assertEquals(group.getId(), returnedGroup.getId());
        // assert that the group contains fields that are admin-only
        assertNotNull(returnedGroup.getManagers());

    }

    /**
     * The test will assert that there is no group listed for the authenticated user because the group of users was
     * created without a manager.
     *
     * @throws IOException if the response body cannot be parsed
     */
    @Test
    void testGetGroupsWithoutManager() throws IOException {
        // given
        assertTrue(groupRepository.findAll().isEmpty());
        List<User> users = getGroupUsers();
        Group group = Group.builder().tenant(getTestTenant()).name("test").users(users).build();
        userRepository.saveAll(users);
        groupRepository.save(group);
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeadersWithRole(Role.MANAGER));
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<Group> groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(groups.isEmpty());
    }

    @Test
    void testGetGroupById() throws IOException {
        // given
        assertTrue(groupRepository.findAll().isEmpty());
        List<User> users = getGroupUsers();
        Group group = Group.builder()
                .tenant(getTestTenant())
                .name("test")
                .users(users)
                .build();
        userRepository.saveAll(users);
        group = groupRepository.save(group);
        // when: Try to get a group with headers that contain the ADMIN role
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT + '/' + group.getId(), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        Group foundGroup = mapper.readValue(response.getBody(), Group.class);
        // then: The authenticated user is allowed to get the group
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                group.getName(), foundGroup.getName(),
                () -> "Group name of group looked up by id does not match saved (expected) group name."
        );

        // when: We try the same with a CU, who has a sub-user that belongs to the group.
        HttpHeaders compositeUserHeaders1 = setCUWithUserAndGetHTTPHeaders(
                users.get(0),
                users.get(0).getAuthId() + "CU"
        );
        entity = new HttpEntity<>(null, compositeUserHeaders1);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT + '/' + group.getId(), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        // then: Regular users (where role is USER) are not allowed to call this endpoint. Even though the CU has a
        // sub-user that belongs to that group, they cannot call this endpoint, since they are still a regular user.
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testGetGroupByNonExistingId() throws IOException {
        // given
        long nonExistingGroupId = 2L;
        assertTrue(groupRepository.findAll().isEmpty());
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT + '/' + nonExistingGroupId, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(
                String.format(GroupNotFoundException.GROUP_NOT_FOUND_RESPONSE_MESSAGE_ID, nonExistingGroupId),
                response.getBody()
        );
    }

    /**
     * Verifies that each of the groups we get back will match the tenant for our authenticated user.
     * If this does not hold, it means we got back groups for a different tenant, which should never happen.
     */
    @Test
    void testGetGroupsForTenant() throws IOException {
        // given
        AccessScopeContext.INSTANCE.setTenantId(2L); // so that we are allowed to create for tenant 2
        Tenant tenant = tenantRepository.findById(2L).get(); // assumption is that it was created by migration
        User user = User.builder().tenant(tenant).build();
        Group group = Group.builder().tenant(tenant).name("testGroup").users(List.of(user)).build();
        userRepository.save(user);
        groupRepository.save(group);
        assertFalse(groupRepository.findAll().isEmpty());
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());
        assertTrue(groupRepository.findAll().isEmpty()); // no groups for default test tenant
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<Group> groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(groups.isEmpty());
    }

    @Test
    void testGetGroupsAcrossTenantsForCU() throws JsonProcessingException {
        // given: We create 3 groups, across 3 different tenants. Each group contains a single user. A composite user is
        // created with 2 of those users as sub-users.
        AccessScopeContext.INSTANCE.setTenantId(1L);
        Tenant tenant1 = tenantRepository.findById(1L).get();
        User user1 = User.builder()
                .tenant(tenant1)
                .build();
        Group group1 = Group.builder()
                .tenant(tenant1)
                .name("testGetGroupsCU1")
                .users(List.of(user1))
                .build();
        userRepository.save(user1);
        groupRepository.save(group1);
        assertFalse(groupRepository.findAll().isEmpty());

        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant2 = tenantRepository.findById(2L).get();
        User user2 = User.builder()
                .tenant(tenant2)
                .build();
        Group group2 = Group.builder()
                .tenant(tenant2)
                .name("testGetGroupsCU2")
                .users(List.of(user2))
                .build();
        userRepository.save(user2);
        groupRepository.save(group2);
        assertFalse(groupRepository.findAll().isEmpty());

        AccessScopeContext.INSTANCE.setTenantId(3L);
        Tenant tenant3 = tenantRepository.findById(3L).get();
        User user3 = User.builder()
                .tenant(tenant3)
                .build();
        Group group3 = Group.builder()
                .tenant(tenant3)
                .name("testGetGroupsCU3")
                .users(List.of(user3))
                .build();
        userRepository.save(user3);
        groupRepository.save(group3);
        assertFalse(groupRepository.findAll().isEmpty());

        // Switch through the tenants and add their users to the CU's sub-users.
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(1L);
        setCUWithUserAndGetHTTPHeaders(user1, "testGetGroupsCU");
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(2L);
        HttpHeaders headers = setCUWithUserAndGetHTTPHeaders(user2, "testGetGroupsCU");

        // when
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<Group> groups = mapper.readValue(response.getBody(), new TypeReference<List<Group>>() {
        });
        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, groups.size());
    }

    @Test
    void testSetGroupLimitGreaterThanTenantLimit() throws JsonProcessingException {
        // given
        AccessScopeContext.INSTANCE.setTenantId(TENANT_WITH_USER_LIMIT_TENANT_ID);

        Group group1 = Group.builder()
                .tenant(getTestTenantWithUserLimit())
                .managers(List.of(authenticatedUserWithLimit))
                .name("test_group_no_limit_1")
                .thingGroupName("test_thing_group")
                .build();
        groupRepository.save(group1);
        Group group2 = Group.builder()
                .tenant(getTestTenantWithUserLimit())
                .managers(List.of(authenticatedUserWithLimit))
                .name("test_group_no_limit_2")
                .thingGroupName("test_thing_group")
                .build();
        groupRepository.save(group2);

        // when

        // use limited tenant's authenticated user (which manages the group) to (attempt to) create the users
        HttpHeaders headers = TestUtils.withAuthenticatedUserHttpHeaders(authenticatedUserWithLimit);

        UserLimit form1 = UserLimit.builder()
                .userLimit(DEFAULT_USER_LIMIT)
                .build();
        HttpEntity<String> entity1 = new HttpEntity<>(mapper.writeValueAsString(form1), headers);
        ResponseEntity<String> responseUpdateGroupLimit1 = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        GROUPS_REST_ENDPOINT + "/" + group1.getId() + USER_LIMIT_REST_ENDPOINT,
                        getPort()
                ),
                HttpMethod.PUT,
                entity1,
                String.class
        );
        UserLimit form2 = UserLimit.builder()
                .userLimit(DEFAULT_USER_LIMIT)
                .build();
        HttpEntity<String> entity2 = new HttpEntity<>(mapper.writeValueAsString(form2), headers);
        ResponseEntity<String> responseUpdateGroupLimit2 = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        GROUPS_REST_ENDPOINT + "/" + group2.getId() + USER_LIMIT_REST_ENDPOINT,
                        getPort()
                ),
                HttpMethod.PUT,
                entity2,
                String.class
        );
        //then
        assertEquals(HttpStatus.OK, responseUpdateGroupLimit1.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, responseUpdateGroupLimit2.getStatusCode());
    }

    private HttpHeaders setCUWithUserAndGetHTTPHeaders(@Nullable User user, String cuAuthId) {
        // We find/create a composite user and, if such is provided, link them to a sub-user.
        CompositeUser compositeUser = compositeUserRepository.findByAuthId(cuAuthId)
                .orElseGet(() -> compositeUserRepository.save(CompositeUser.builder()
                        .authId(cuAuthId)
                        .build()));
        if (user != null) {
            user.setCompositeUser(compositeUser);
            user = userRepository.save(user);
        }

        // We make the same request but authenticate as the composite user
        AccessScopeContext.INSTANCE.setTenantId(null);
        AccessScopeContext.INSTANCE.setUserType(UserType.COMPOSITE_USER);
        AccessScopeContext.INSTANCE.setCompositeUserId(compositeUser.getId());
        return new TestUtils.TestHttpHeadersBuilder()
                .withUserType(UserType.COMPOSITE_USER)
                .withAuthId(compositeUser.getAuthId())
                .withRole(Role.USER)
                .withCompositeUserId(compositeUser.getId())
                .build();
    }
}
