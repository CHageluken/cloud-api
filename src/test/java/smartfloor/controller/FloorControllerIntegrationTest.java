package smartfloor.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.FloorRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.util.TestUtils;

class FloorControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    FloorRepository floorRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CompositeUserRepository compositeUserRepository;
    @Autowired
    GroupRepository groupRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        authenticatedUser = getTestUser();
    }

    @Test
    void testGetFloor() throws IOException {
        Floor floor = Floor.builder().name("floor").viewers(List.of(authenticatedUser)).build();
        floor = floorRepository.save(floor);

        HttpEntity<String> entity = new HttpEntity<>(floor.getId().toString(), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/floors/" + floor.getId(), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        Floor floorFromResponse = mapper.readValue(response.getBody(), Floor.class);
        assertEquals(floor.getId(), floorFromResponse.getId());

        // Repeat test for composite user. Same outcome is expected.
        HttpHeaders compositeUserHeaders =
                setCUWithUserAndGetHTTPHeaders(authenticatedUser, authenticatedUser.getAuthId() + "CU");
        entity = new HttpEntity<>(floor.getId().toString(), compositeUserHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/floors/" + floor.getId(), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        floorFromResponse = mapper.readValue(response.getBody(), Floor.class);
        assertEquals(floor.getId(), floorFromResponse.getId());

    }

    /**
     * Test that checks the access to floors.
     * There are two ways to provide access to a floor - through the usage of the table floor_viewers (which links
     * individual users to a floor), and through floor_groups (which links a group (and therefore all its members) to a
     * floor).
     * This test evaluates the floor access of different users (regular, manager, composite, etc.) and ensures the type
     * of their access (through floor_viewers, floor_groups or both) does not matter.
     */
    @Test
    void testGetOnlyAccessibleFloors() throws IOException {
        // given
        int initialNumberOfFloors = floorRepository.findByViewer(authenticatedUser).size();
        // First floor is accessible only to the default test user through the floor_viewers table
        Floor f1 = Floor.builder().name("floorOfTestUser").viewers(List.of(authenticatedUser)).build();
        floorRepository.save(f1);

        User subUser = User.builder()
                .tenant(Tenant.getDefaultTenant())
                .authId("userWithDifferentCU")
                .build();
        userRepository.save(subUser);

        User gm1 = User.builder()
                .tenant(getTestTenant())
                .authId("gmWithAccess")
                .build();
        userRepository.save(gm1);
        Group g1 = Group.builder()
                .name("groupWithFloorAccess")
                .tenant(getTestTenant())
                .users(List.of(authenticatedUser, subUser))
                .managers(List.of(gm1))
                .build();
        g1 = groupRepository.save(g1);
        // The second floor is also accessible only to the default test user through the floor_groups table
        Floor f2 = Floor.builder().name("groupFloor").groups(List.of(g1)).build();
        floorRepository.save(f2);
        // The third floor has no direct viewers and no groups. This makes it inaccessible.
        Floor f3 = Floor.builder().name("inaccessibleFloor").viewers(new ArrayList<>()).build();
        floorRepository.save(f3);

        CompositeUser cu = CompositeUser.builder()
                .authId("randomCU")
                .build();
        compositeUserRepository.save(cu);
        subUser.setCompositeUser(cu);
        userRepository.save(subUser);

        // The fourth floor can be accessed only by the CU
        Floor f4 = Floor.builder().name("floorOfCompositeUser").viewers(List.of(subUser)).build();
        floorRepository.save(f4);

        AccessScopeContext.INSTANCE.setTenantId(1L);
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);

        // when: We get all floors for the default test user
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/floors", getPort()), HttpMethod.GET, entity, String.class);

        // then: The test user should have access to two new floors
        List<Floor> floorsFromResponse = mapper.readValue(response.getBody(), new TypeReference<List<Floor>>() {
        });
        assertEquals(initialNumberOfFloors + 2, floorsFromResponse.size());
        List<Floor> inaccessibleFloorsFromResponse = floorsFromResponse.stream()
                .filter(f -> f.getName().contains("inaccessible") || f.getName().contains("Composite"))
                .collect(Collectors.toUnmodifiableList());
        assertEquals(0, inaccessibleFloorsFromResponse.size());

        // then when: Test the same endpoint, this time authenticating as the composite user.
        HttpHeaders compositeUserHeaders1 = setCUWithUserAndGetHTTPHeaders(
                subUser, subUser.getAuthId() + "CU"
        );
        entity = new HttpEntity<>(null, compositeUserHeaders1);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/floors", getPort()), HttpMethod.GET, entity, String.class);

        // then: The CU should have access to two new floors
        floorsFromResponse = mapper.readValue(response.getBody(), new TypeReference<List<Floor>>() {
        });
        assertEquals(2, floorsFromResponse.size());
        inaccessibleFloorsFromResponse = floorsFromResponse.stream()
                .filter(f -> f.getName().contains("inaccessible") || f.getName().contains("TestUser"))
                .collect(Collectors.toUnmodifiableList());
        assertEquals(0, inaccessibleFloorsFromResponse.size());

        AccessScopeContext.INSTANCE.setTenantId(1L);
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);

        // then when: We call the same endpoint, authenticated as the group manager, who has access to their group's
        // floors.
        HttpHeaders gmHeaders =
                TestUtils.httpHeadersBuilder(getTestTenant().getId(), gm1, Role.MANAGER, UserType.DIRECT_USER);
        entity = new HttpEntity<>(null, gmHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/floors", getPort()), HttpMethod.GET, entity, String.class);

        // then: The group manager should have access to only one floor
        floorsFromResponse = mapper.readValue(response.getBody(), new TypeReference<List<Floor>>() {
        });
        assertEquals(1, floorsFromResponse.size());
        assertEquals("groupFloor", floorsFromResponse.get(0).getName());

        // then when: Test the same endpoint, this time authenticating as a different composite user. This CU has the
        // default test user as a sub-user. This way we also ensure that CUs are isolated too.
        HttpHeaders compositeUserHeaders2 = setCUWithUserAndGetHTTPHeaders(
                authenticatedUser, authenticatedUser.getAuthId() + "CU"
        );
        entity = new HttpEntity<>(null, compositeUserHeaders2);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/floors", getPort()), HttpMethod.GET, entity, String.class);

        // then
        floorsFromResponse = mapper.readValue(response.getBody(), new TypeReference<List<Floor>>() {
        });
        assertEquals(initialNumberOfFloors + 2, floorsFromResponse.size());
        inaccessibleFloorsFromResponse = floorsFromResponse.stream()
                .filter(f -> f.getName().contains("inaccessible") || f.getName().contains("Composite"))
                .collect(Collectors.toUnmodifiableList());
        assertEquals(0, inaccessibleFloorsFromResponse.size());
    }


    @Test
    void testGetNonExistingFloor() throws IOException {
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format("/floors/%d", 500L), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );

        assertEquals(
                404, response.getStatusCode().value(),
                () -> "Expected 404 NOT FOUND but response status code was different."
        );
    }

    @Test
    void testGetInaccessibleFloor() throws IOException {
        // given
        Floor inaccessibleFloor =
                Floor.builder().name("inaccessible").viewers(new ArrayList<>()).groups(new ArrayList<>()).build();
        inaccessibleFloor = floorRepository.save(inaccessibleFloor);

        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format("/floors/%d", inaccessibleFloor.getId()), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then
        assertEquals(
                403, response.getStatusCode().value(),
                () -> "Expected 403 FORBIDDEN but response status code was different."
        );

        // Test the same with a composite user.
        HttpHeaders compositeUserHeaders = setCUWithUserAndGetHTTPHeaders(
                authenticatedUser, authenticatedUser.getAuthId() + "CU"
        );
        entity = new HttpEntity<>(null, compositeUserHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format("/floors/%d", inaccessibleFloor.getId()), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then
        assertEquals(
                403, response.getStatusCode().value(),
                () -> "Expected 403 FORBIDDEN but response status code was different."
        );
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
