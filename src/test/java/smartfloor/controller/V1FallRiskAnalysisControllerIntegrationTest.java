package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.V1FallRiskScoreAssessment;
import smartfloor.domain.indicators.Indicator;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.FallRiskProfileRepository;
import smartfloor.repository.jpa.FallRiskRemovalRepository;
import smartfloor.repository.jpa.FloorRepository;
import smartfloor.repository.jpa.FootstepRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.UserWearableLinkRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

class V1FallRiskAnalysisControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    WearableRepository wearableRepository;
    @Autowired
    FloorRepository floorRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserWearableLinkRepository userWearableLinkRepository;
    @Autowired
    FootstepRepository footstepRepository;
    @Autowired
    FallRiskProfileRepository fallRiskProfileRepository;
    @Autowired
    FallRiskRemovalRepository fallRiskRemovalRepository;
    @Autowired
    WearableGroupRepository wearableGroupRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    CompositeUserRepository compositeUserRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * TODO.
     */
    Wearable getTestWearable() {
        Wearable w = wearableRepository.findById("testWearableForFallRiskV1");
        if (w == null) {
            w = Wearable.builder()
                    .id("testWearableForFallRiskV1")
                    .build();
            w = wearableRepository.save(w);
        }
        return w;
    }

    /**
     * TODO.
     */
    WearableGroup getTestWearableGroup() {
        return wearableGroupRepository.findByName("testWearableGroupForFallRiskV1")
                .orElseGet(() -> wearableGroupRepository.save(
                        WearableGroup.builder()
                                .name("testWearableGroupForFallRiskV1")
                                .wearables(List.of(getTestWearable()))
                                .build()
                ));
    }

    /**
     * TODO.
     */
    Group getTestGroup(List<User> users) {
        return groupRepository.findByName("testGroupForFallRiskV1")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .users(users)
                                .name("testGroupForFallRiskV1")
                                .build())
                );
    }

    @Test
    void testGetUserLatestFallRiskProfileWithIndicators() throws JsonProcessingException {
        String endpoint = "/v1/analyses/fall-risk/latest/users/";
        Tenant tenant = getTestTenant();
        Floor floor = Floor.builder().name("FRP v1 test floor").build();
        floor = floorRepository.save(floor);
        Wearable wearable = Wearable.builder().id("wearableForFRPv1").build();
        wearable = wearableRepository.save(wearable);
        WearableGroup wg = WearableGroup.builder()
                .wearables(List.of(wearable))
                .name("wg_ltst_frp_with_indicators")
                .build();
        wearableGroupRepository.save(wg);

        User firstUser = User.builder().authId("first user").tenant(tenant).build();
        firstUser = userRepository.save(firstUser);
        User secondUser = User.builder().authId("second user").tenant(tenant).build();
        secondUser = userRepository.save(secondUser);

        Group g = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .users(List.of(firstUser, secondUser))
                .wearableGroup(wg)
                .name("group_ltst_frp_with_indicators")
                .build();
        groupRepository.save(g);

        LocalDateTime timeNow = LocalDateTime.now();
        long uwlDurationInM = 20;
        LocalDateTime firstUserUWLBeginTime =
                timeNow.minusDays(3).atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime firstUserUWLEndTime = timeNow.minusDays(3)
                .plusMinutes(uwlDurationInM)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime firstRandomTimestampOfNoUWLs =
                timeNow.minusDays(2).atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime secondUserUWLBeginTime =
                timeNow.minusDays(1).atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime secondUserUWLEndTime = timeNow.minusDays(1)
                .plusMinutes(uwlDurationInM)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime secondRandomTimestampOfNoUWLs =
                timeNow.minusMinutes(30).atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink firstUserUWL = new UserWearableLink();
        firstUserUWL.setBeginTime(firstUserUWLBeginTime);
        firstUserUWL.setEndTime(firstUserUWLEndTime);
        firstUserUWL.setUser(firstUser);
        firstUserUWL.setWearable(wearable);
        userWearableLinkRepository.save(firstUserUWL);

        UserWearableLink secondUserUWL = new UserWearableLink();
        secondUserUWL.setBeginTime(secondUserUWLBeginTime);
        secondUserUWL.setEndTime(secondUserUWLEndTime);
        secondUserUWL.setUser(secondUser);
        secondUserUWL.setWearable(wearable);
        userWearableLinkRepository.save(secondUserUWL);

        FallRiskProfile firstUserFirstFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(firstUserUWLBeginTime)
                .beginTime(firstUserUWLBeginTime).endTime(firstUserUWLEndTime.minusMinutes(15))
                .notes("first user, 1st FRP")
                .walkingSpeed(1.0).stepLength(2.0).stepFrequency(3.0).rmsVerticalAcceleration(4.0).build();

        FallRiskProfile firstUserSecondFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(firstUserUWLEndTime.minusMinutes(20))
                .beginTime(firstUserUWLEndTime.minusMinutes(20)).endTime(firstUserUWLEndTime)
                .notes("first user, 2nd FRP")
                .walkingSpeed(1.0).stepLength(2.0).stepFrequency(3.0).rmsVerticalAcceleration(5.0).build();

        FallRiskProfile firstNonUserFRP = FallRiskProfile.builder()
                .wearable(wearable)
                .floor(floor)
                .creationTime(firstRandomTimestampOfNoUWLs)
                .beginTime(firstRandomTimestampOfNoUWLs)
                .endTime(firstRandomTimestampOfNoUWLs.plusMinutes(uwlDurationInM))
                .notes("first NON user FRP")
                .walkingSpeed(5.0)
                .stepLength(6.0)
                .stepFrequency(7.0)
                .rmsVerticalAcceleration(6.0)
                .build();

        FallRiskProfile secondUserFirstFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(secondUserUWLBeginTime)
                .beginTime(secondUserUWLBeginTime).endTime(secondUserUWLEndTime.minusMinutes(15))
                .notes("second user, 1st FRP")
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(7.0).build();

        FallRiskProfile secondUserSecondFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(secondUserUWLEndTime.minusMinutes(10))
                .beginTime(secondUserUWLEndTime.minusMinutes(10)).endTime(secondUserUWLEndTime)
                .notes("second user, 2nd FRP")
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).build();

        FallRiskProfile secondNonUserFRP = FallRiskProfile.builder()
                .wearable(wearable)
                .floor(floor)
                .creationTime(secondRandomTimestampOfNoUWLs)
                .beginTime(secondRandomTimestampOfNoUWLs)
                .endTime(secondRandomTimestampOfNoUWLs.plusMinutes(uwlDurationInM))
                .notes("second NON user FRP")
                .walkingSpeed(10.0)
                .stepLength(11.0)
                .stepFrequency(12.0)
                .rmsVerticalAcceleration(9.0)
                .build();

        fallRiskProfileRepository.saveAll(List.of(firstUserFirstFRP, firstUserSecondFRP, firstNonUserFRP,
                secondUserFirstFRP, secondUserSecondFRP, secondNonUserFRP
        ));

        // Request latest FRP of first user
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(endpoint + firstUser.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());

        V1FallRiskScoreAssessment firstUserLatestFRP =
                mapper.readValue(response.getBody(), new TypeReference<V1FallRiskScoreAssessment>() {
                });

        // Assert that the latest FRP contains only the expected fields
        assertNotNull(firstUserLatestFRP.getUser());
        assertNotNull(firstUserLatestFRP.getFloor());
        assertNotNull(firstUserLatestFRP.getBeginTime());
        assertNotNull(firstUserLatestFRP.getEndTime());
        assertNotNull(firstUserLatestFRP.getIndicators());

        // Assert on indicator values
        V1FallRiskScoreAssessment expectedLatest =
                new V1FallRiskScoreAssessment(firstUser, firstUserSecondFRP);
        List<Indicator> expectedLatestIndicators = expectedLatest.getIndicators();
        for (int i = 0; i < expectedLatestIndicators.size() - 1; i++) {
            assertEquals(
                    expectedLatestIndicators.get(i).getValue(),
                    firstUserLatestFRP.getIndicators().get(i).getValue()
            );
        }

        // Link the users to a composite user
        CompositeUser compositeUser = CompositeUser.builder()
                .authId("testV1FRSACU")
                .build();
        compositeUser = compositeUserRepository.save((compositeUser));
        firstUser.setCompositeUser(compositeUser);
        userRepository.save(firstUser);
        HttpHeaders cuHeaders = setCUWithUserAndGetHTTPHeaders(secondUser, compositeUser.getAuthId());

        // Make the same request but change the headers
        entity = new HttpEntity<>(null, cuHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(endpoint + firstUser.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());

        // Expect the same result
        firstUserLatestFRP = mapper.readValue(response.getBody(), new TypeReference<V1FallRiskScoreAssessment>() {
        });
        for (int i = 0; i < expectedLatestIndicators.size() - 1; i++) {
            assertEquals(
                    expectedLatestIndicators.get(i).getValue(),
                    firstUserLatestFRP.getIndicators().get(i).getValue()
            );
        }
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
