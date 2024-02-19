package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
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

class AnalysisControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    WearableRepository wearableRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserWearableLinkRepository userWearableLinkRepository;
    @Autowired
    FootstepRepository footstepRepository;
    @Autowired
    WearableGroupRepository wearableGroupRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    CompositeUserRepository compositeUserRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Clear the test database of any footsteps so that we make sure to have a clean start for every test case.
     */
    @BeforeEach
    void setUp() {
        footstepRepository.deleteAll();
    }

    @Test
    void testGetFrequency() throws IOException {
        User user = User.builder()
                .tenant(getTestTenant())
                .authId("testGetFrequencyUser")
                .build();
        user = userRepository.save(user);

        Wearable wearable = Wearable.builder()
                .id("testGetFrequencyWearable")
                .build();
        wearable = wearableRepository.save(wearable);

        WearableGroup wg = WearableGroup.builder()
                .name("getFreqWG")
                .wearables(List.of(wearable))
                .build();
        wg = wearableGroupRepository.save(wg);

        Group g = Group.builder()
                .users(List.of(user))
                .wearableGroup(wg)
                .tenant(getTestTenant())
                .name("getFreqG")
                .build();
        groupRepository.save(g);

        final LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(currentTime.minusDays(1))
                .endTime(currentTime)
                .user(user)
                .wearable(wearable)
                .build();
        userWearableLinkRepository.save(userWearableLink);

        Footstep footstep1 = new Footstep();
        footstep1.setWearable(wearable);
        footstep1.setPosition(new Position(1, 1));
        footstep1.setTime(currentTime.minusSeconds(1));
        Footstep footstep2 = new Footstep();
        footstep2.setWearable(wearable);
        footstep2.setPosition(new Position(1, 2));
        footstep2.setTime(currentTime);
        List<Footstep> footsteps = new ArrayList<>();
        footsteps.add(footstep1);
        footsteps.add(footstep2);
        footstepRepository.saveAll(footsteps);

        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format(
                        "/analyses/stride-frequency/users/%d?begin=%d&end=%d",
                        user.getId(),
                        currentTime.minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                ), getPort()),

                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());
        double frequency = extractIndicatorValueFromResponseEntity(response);
        assertEquals(2, frequency, 0.000000);

        HttpHeaders headers = setCUWithUserAndGetHTTPHeaders(user, user.getAuthId() + "CU");
        entity = new HttpEntity<>(null, headers);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format(
                        "/analyses/stride-frequency/users/%d?begin=%d&end=%d",
                        user.getId(),
                        currentTime.minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                ), getPort()),

                HttpMethod.GET, entity, String.class
        );
        frequency = extractIndicatorValueFromResponseEntity(response);
        // then: Expect the same result
        assertEquals(2, frequency, 0.000000);
    }

    @Test
    void testGetDistance() throws IOException {
        User user = User.builder()
                .tenant(getTestTenant())
                .authId("testGetDistanceUser")
                .build();
        user = userRepository.save(user);

        Wearable wearable = Wearable.builder()
                .id("testGetDistanceWearable")
                .build();
        wearable = wearableRepository.save(wearable);

        WearableGroup wg = WearableGroup.builder()
                .name("getDistanceWG")
                .wearables(List.of(wearable))
                .build();
        wg = wearableGroupRepository.save(wg);

        Group g = Group.builder()
                .users(List.of(user))
                .wearableGroup(wg)
                .tenant(getTestTenant())
                .name("getDistanceG")
                .build();
        groupRepository.save(g);
        final LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(currentTime.minusDays(2))
                .endTime(currentTime)
                .user(user)
                .wearable(wearable)
                .build();
        userWearableLink = userWearableLinkRepository.save(userWearableLink);

        Footstep footstep1 = new Footstep();
        footstep1.setWearable(wearable);
        footstep1.setPosition(new Position(1, 1));
        footstep1.setTime(currentTime.minusSeconds(1));
        Footstep footstep2 = new Footstep();
        footstep2.setWearable(wearable);
        footstep2.setPosition(new Position(1, 2));
        footstep2.setTime(currentTime);
        List<Footstep> footsteps = new ArrayList<>();
        footsteps.add(footstep1);
        footsteps.add(footstep2);
        footstepRepository.saveAll(footsteps);

        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format(
                        "/analyses/distance/cumulative/users/%d?begin=%d&end=%d",
                        user.getId(),
                        currentTime.minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                ), getPort()),

                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());
        double distance = extractIndicatorValueFromResponseEntity(response);
        assertEquals(1, distance, 0.000000);

        // then given: We create a composite user and link the existing user to them
        HttpHeaders headers = setCUWithUserAndGetHTTPHeaders(user, user.getAuthId() + "CU");
        entity = new HttpEntity<>(null, headers);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format(
                        "/analyses/distance/cumulative/users/%d?begin=%d&end=%d",
                        user.getId(),
                        currentTime.minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                ), getPort()),

                HttpMethod.GET, entity, String.class
        );
        distance = extractIndicatorValueFromResponseEntity(response);
        // then: Expect the same result
        assertEquals(1, distance, 0.000000);
    }

    @Test
    void testGetAverageSpeed() throws IOException {
        User user = User.builder()
                .authId("testUserAuthForAvgSpeed")
                .tenant(getTestTenant())
                .build();
        user = userRepository.save(user);

        Wearable wearable = Wearable.builder()
                .id("testWearableForAvgSpeed")
                .build();
        wearable = wearableRepository.save(wearable);

        WearableGroup wg = WearableGroup.builder()
                .name("getAverageSpeedWG")
                .wearables(List.of(wearable))
                .build();
        wg = wearableGroupRepository.save(wg);

        Group g = Group.builder()
                .users(List.of(user))
                .wearableGroup(wg)
                .tenant(getTestTenant())
                .name("getAverageSpeedG")
                .build();
        groupRepository.save(g);

        final LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(currentTime.minusDays(2))
                .endTime(currentTime)
                .wearable(wearable)
                .user(user)
                .build();
        userWearableLink = userWearableLinkRepository.save(userWearableLink);

        Footstep footstep1 = new Footstep();
        footstep1.setWearable(wearable);
        footstep1.setPosition(new Position(1, 1));
        footstep1.setTime(currentTime.minusSeconds(1));
        Footstep footstep2 = new Footstep();
        footstep2.setWearable(wearable);
        footstep2.setPosition(new Position(1, 2));
        footstep2.setTime(currentTime);
        List<Footstep> footsteps = new ArrayList<>();
        footsteps.add(footstep1);
        footsteps.add(footstep2);
        footstepRepository.saveAll(footsteps);

        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(String.format(
                        "/analyses/speed/average/users/%d?begin=%d&end=%d",
                        user.getId(),
                        currentTime.minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli(),
                        currentTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                ), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());
        double speed = extractIndicatorValueFromResponseEntity(response);
        assertEquals(1.0, speed, 0.000001);
    }

    private double extractIndicatorValueFromResponseEntity(ResponseEntity<String> response)
            throws JsonProcessingException {
        Object resp = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        Map respMap = mapper.convertValue(resp, Map.class);
        return (double) respMap.get("value");
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
