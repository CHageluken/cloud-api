package smartfloor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.GeoModule;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Position;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.FootstepRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.UserWearableLinkRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

class FootstepControllerIntegrationTest extends IntegrationTestBase {
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    FootstepRepository footstepRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WearableRepository wearableRepository;
    @Autowired
    UserWearableLinkRepository userWearableLinkRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    WearableGroupRepository wearableGroupRepository;
    @Autowired
    CompositeUserRepository compositeUserRepository;

    /**
     * TODO.
     */
    Wearable getTestWearable() {
        Wearable w = wearableRepository.findById("testWearableForFootsteps");
        if (w == null) {
            w = Wearable.builder()
                    .id("testWearableForFootsteps")
                    .build();
            w = wearableRepository.save(w);
        }
        return w;
    }

    /**
     * TODO.
     */
    WearableGroup getTestWearableGroup() {
        return wearableGroupRepository.findByName("testWearableGroupForFootsteps")
                .orElseGet(() -> wearableGroupRepository.save(
                        WearableGroup.builder()
                                .name("testWearableGroupForFootsteps")
                                .wearables(List.of(getTestWearable()))
                                .build()
                ));
    }

    /**
     * TODO.
     */
    Group getTestGroup() {
        return groupRepository.findByName("testGroupForFootsteps")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .name("testGroupForFootsteps")
                                .build())
                );
    }

    /**
     * TODO.
     */
    Group getTestGroup(Tenant t, List<User> users) {
        return groupRepository.findByName("testGroupForFootsteps")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(t)
                                .users(users)
                                .name("testGroupForFootsteps")
                                .build())
                );
    }

    /**
     * TODO.
     */
    Group getTestGroup(List<User> users) {
        return groupRepository.findByName("testGroupForFootsteps")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .users(users)
                                .name("testGroupForFootsteps")
                                .build())
                );
    }


    @Test
    void testGetFootstepCountsWithinTimeWindow() throws IOException {
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testGetFootstepCounts").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        final LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        Footstep footstep1 = new Footstep();
        footstep1.setWearable(wearable);
        footstep1.setPosition(new Position(1, 1));
        footstep1.setTime(currentTime.minusSeconds(2));
        Footstep footstep2 = new Footstep();
        footstep2.setWearable(wearable);
        footstep2.setPosition(new Position(1, 2));
        footstep2.setTime(currentTime);
        List<Footstep> footsteps = new ArrayList<>();
        footsteps.add(footstep1);
        footsteps.add(footstep2);
        footstepRepository.saveAll(footsteps);

        UserWearableLink userWearableLink = new UserWearableLink();
        userWearableLink.setBeginTime(currentTime.minusDays(3));
        userWearableLink.setEndTime(currentTime);
        userWearableLink.setUser(user);
        userWearableLink.setWearable(wearable);
        userWearableLinkRepository.save(userWearableLink);
        // when: We request footstep count, authenticated as a direct user.
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        String.format(
                                "/footsteps/count?begin=%d&end=%d",
                                currentTime.minusDays(3).toInstant(ZoneOffset.UTC).toEpochMilli(),
                                currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ), HttpMethod.GET, entity, String.class);
        mapper.registerModule(new GeoModule());
        List<Footstep> returnedFootsteps = mapper.readValue(response.getBody(), List.class);
        // then: We expect the footsteps to be returned, since they are associated to a user, who belongs to the current
        // tenant.
        assertNotEquals(0, returnedFootsteps.size());

        // when: Try to get the same footsteps authenticated as a CU, this time having the test user as a sub-user.
        HttpHeaders compositeUserHeaders1 = setCUWithUserAndGetHTTPHeaders(user, "testGetFootstepCountCU1");
        entity = new HttpEntity<>(null, compositeUserHeaders1);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        String.format(
                                "/footsteps/count?begin=%d&end=%d",
                                currentTime.minusDays(3).toInstant(ZoneOffset.UTC).toEpochMilli(),
                                currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ), HttpMethod.GET, entity, String.class);
        mapper.registerModule(new GeoModule());
        returnedFootsteps = mapper.readValue(response.getBody(), List.class);
        // then: The footsteps should be present.
        assertNotEquals(0, returnedFootsteps.size());

        // when: Try to get the same footsteps but this time authenticated as a CU with no sub-users.
        HttpHeaders compositeUserHeaders = setCUWithUserAndGetHTTPHeaders(null, "testGetFootstepCountCU2");
        entity = new HttpEntity<>(null, compositeUserHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        String.format(
                                "/footsteps/count?begin=%d&end=%d",
                                currentTime.minusDays(3).toInstant(ZoneOffset.UTC).toEpochMilli(),
                                currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ), HttpMethod.GET, entity, String.class);
        mapper.registerModule(new GeoModule());
        returnedFootsteps = mapper.readValue(response.getBody(), List.class);
        // then: We expect no footsteps to be returned, since this CU has no sub-users.
        assertEquals(0, returnedFootsteps.size());
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
