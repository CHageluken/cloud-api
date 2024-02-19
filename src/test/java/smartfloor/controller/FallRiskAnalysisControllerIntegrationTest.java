package smartfloor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.GeoModule;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Position;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.domain.entities.fall.risk.profile.FallRiskAssessmentModel;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskScoreAssessment;
import smartfloor.domain.entities.fall.risk.profile.LatestFallRiskProfileAssessment;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.FallRiskProfileRepository;
import smartfloor.repository.jpa.FloorRepository;
import smartfloor.repository.jpa.FootstepRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.UserWearableLinkRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

class FallRiskAnalysisControllerIntegrationTest extends IntegrationTestBase {

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
    WearableGroupRepository wearableGroupRepository;
    @Autowired
    CompositeUserRepository compositeUserRepository;
    @Autowired
    private GroupRepository groupRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * TODO.
     */
    Wearable getTestWearable() {
        Wearable w = wearableRepository.findById("testWearableForFallRisk");
        if (w == null) {
            w = Wearable.builder()
                    .id("testWearableForFallRisk")
                    .build();
            w = wearableRepository.save(w);
        }
        return w;
    }

    /**
     * TODO.
     */
    WearableGroup getTestWearableGroup() {
        return wearableGroupRepository.findByName("testWearableGroupForFallRisk")
                .orElseGet(() -> wearableGroupRepository.save(
                        WearableGroup.builder()
                                .name("testWearableGroupForFallRisk")
                                .wearables(List.of(getTestWearable()))
                                .build()
                ));
    }

    /**
     * TODO.
     */
    Group getTestGroup(List<User> users) {
        return groupRepository.findByName("testGroupForFallRisk")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .users(users)
                                .name("testGroupForFallRisk")
                                .build())
                );
    }

    /**
     * Clear the test database of any footsteps so that we make sure to have a clean start for every test case.
     */
    @BeforeEach
    void setUp() {
        footstepRepository.deleteAll();
        fallRiskProfileRepository.deleteAll();
        userWearableLinkRepository.deleteAll();
        groupRepository.deleteAll();
        floorRepository.deleteAll();
    }

    /**
     * Test if the correct fall risk profiles (FRPs) are returned given UWLs and FRPs with different time windows. The
     * FRPs should be returned if their time window (beginTime, endTime) overlaps with the UWLs time window.
     */
    @Test
    void testGetFallRiskAssessmentsWithinTimeWindows() throws IOException {
        Tenant tenant = getTestTenant();
        Floor floor = Floor.builder().name("FRP test floor").build();
        floorRepository.save(floor);
        User user = User.builder().tenant(tenant).authId("testUserAuthForFallRisk").build();
        user = userRepository.save(user);
        Group g = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        Footstep firstFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 1))
                .time(LocalDateTime.now().minusSeconds(1).truncatedTo(ChronoUnit.MILLIS))
                .build();
        Footstep secondFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 2))
                .time(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();
        List<Footstep> footsteps = new ArrayList<>(Arrays.asList(firstFootstep, secondFootstep));
        footstepRepository.saveAll(footsteps);

        /* The time window that the user wearable link spans (is valid in). */
        LocalDateTime uwlBeginTime = LocalDateTime.now()
                .minusDays(2)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        /* The creation times of three FRPs of which only the second two should be in the result set. */
        LocalDateTime olderFrpCreationTime = LocalDateTime.now()
                .minusDays(3)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime frpCreationTime = LocalDateTime.now()
                .minusDays(1)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newerFrpCreationTime = LocalDateTime.now()
                .minusDays(3)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = new UserWearableLink();
        userWearableLink.setBeginTime(uwlBeginTime);
        userWearableLink.setEndTime(uwlEndTime);
        userWearableLink.setUser(user);
        userWearableLink.setWearable(wearable);
        userWearableLinkRepository.save(userWearableLink);

        /* We'll create fall risk profiles and save them, although normally the FRPs are inserted by an AWS Lambda. */

        /* The first FRP is "older" than the user wearable link time window and should therefore not be part of the
        result set. */
        FallRiskProfile olderFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(olderFrpCreationTime)
                .beginTime(olderFrpCreationTime).endTime(olderFrpCreationTime.plusMinutes(2))
                .walkingSpeed(1.0).stepLength(2.0).stepFrequency(3.0).rmsVerticalAcceleration(4.0).build();

        /* The second FRP falls within the user wearable link time window and should be part of the result set. */
        FallRiskProfile matchingFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getEndTime())
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).build();

        /* The third FRP falls within the user wearable link time window and should be part of the result set. */
        FallRiskProfile secondMatchingFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime().plusSeconds(1L)).endTime(userWearableLink.getEndTime())
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).build();

        /* The last FRP is "newer" than the user wearable link time window and should therefore not be part of the
        result set. */
        FallRiskProfile newerFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(newerFrpCreationTime)
                .beginTime(newerFrpCreationTime).endTime(newerFrpCreationTime.plusMinutes(3))
                .walkingSpeed(10.0).stepLength(11.0).stepFrequency(12.0).rmsVerticalAcceleration(13.0).build();

        fallRiskProfileRepository.save(olderFRP);
        fallRiskProfileRepository.save(matchingFRP);
        fallRiskProfileRepository.save(secondMatchingFRP);
        fallRiskProfileRepository.save(newerFRP);

        ZonedDateTime utcNow = LocalDateTime.now().atZone(ZoneOffset.UTC);
        ZonedDateTime utcSixMonthsAgo = utcNow.minusMonths(6);

        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() + String.format(
                        "?begin=%d&end=%d",
                        utcSixMonthsAgo.toInstant().toEpochMilli(),
                        utcNow.toInstant().toEpochMilli()
                ), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());

        List<FallRiskAssessmentModel> fallRiskAssessments =
                Arrays.asList(mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class));

        assertNotNull(fallRiskAssessments);
        assertEquals(2, fallRiskAssessments.size());

        List<FallRiskProfile> retrievedFRPs = fallRiskAssessments.stream()
                .map(FallRiskAssessmentModel::getFallRiskProfile)
                .toList();

        /* We ignore the wearable, floor fields in the fall risk profiles returned here as they are not serialized.
        Actually, AssertJ should be able to check wearable.tenant specifically but for some reason this was not working
        while writing this test. */
        Assertions.assertThat(retrievedFRPs.get(0))
                .usingRecursiveComparison()
                .ignoringFields("wearable")
                .ignoringFields("floor")
                .ignoringFields("rmsVerticalAcceleration")
                .ignoringFields("notes")
                .isEqualTo(matchingFRP);
        Assertions.assertThat(retrievedFRPs.get(1))
                .usingRecursiveComparison()
                .ignoringFields("wearable")
                .ignoringFields("floor")
                .ignoringFields("rmsVerticalAcceleration")
                .ignoringFields("notes")
                .isEqualTo(secondMatchingFRP);

        /* Check wearable id's specifically as they were ignored in the previous assertions. */
        assertEquals(retrievedFRPs.get(0).getWearable().getId(), matchingFRP.getWearable().getId());
        assertEquals(retrievedFRPs.get(1).getWearable().getId(), secondMatchingFRP.getWearable().getId());

        /* Check floor id's specifically as they were ignored in the previous assertions. */
        assertEquals(retrievedFRPs.get(0).getFloor().getId(), matchingFRP.getFloor().getId());
        assertEquals(retrievedFRPs.get(1).getFloor().getId(), secondMatchingFRP.getFloor().getId());

        // Repeat the same test, this time authenticating as a CU (with a sub-user).
        HttpHeaders compositeUserHeaders = setCUWithUserAndGetHTTPHeaders(user, user.getAuthId() + "CU");

        entity = new HttpEntity<>(null, compositeUserHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() + String.format(
                        "?begin=%d&end=%d",
                        utcSixMonthsAgo.toInstant().toEpochMilli(),
                        utcNow.toInstant().toEpochMilli()
                ), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());

        fallRiskAssessments = Arrays.asList(mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class));

        assertNotNull(fallRiskAssessments);
        assertEquals(2, fallRiskAssessments.size());

        retrievedFRPs = fallRiskAssessments.stream()
                .map(FallRiskAssessmentModel::getFallRiskProfile)
                .toList();

        Assertions.assertThat(retrievedFRPs.get(0))
                .usingRecursiveComparison()
                .ignoringFields("wearable")
                .ignoringFields("floor")
                .ignoringFields("rmsVerticalAcceleration")
                .ignoringFields("notes")
                .isEqualTo(matchingFRP);
        Assertions.assertThat(retrievedFRPs.get(1))
                .usingRecursiveComparison()
                .ignoringFields("wearable")
                .ignoringFields("floor")
                .ignoringFields("rmsVerticalAcceleration")
                .ignoringFields("notes")
                .isEqualTo(secondMatchingFRP);

        assertEquals(retrievedFRPs.get(0).getWearable().getId(), matchingFRP.getWearable().getId());
        assertEquals(retrievedFRPs.get(1).getWearable().getId(), secondMatchingFRP.getWearable().getId());

        assertEquals(retrievedFRPs.get(0).getFloor().getId(), matchingFRP.getFloor().getId());
        assertEquals(retrievedFRPs.get(1).getFloor().getId(), secondMatchingFRP.getFloor().getId());

        // Repeat the same test, this time authenticating as a different CU (no sub-users).
        HttpHeaders compositeUserHeaders1 = setCUWithUserAndGetHTTPHeaders(null, "testGetFRPsForCUWithoutSubUsers");
        entity = new HttpEntity<>(null, compositeUserHeaders1);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() + String.format(
                        "?begin=%d&end=%d",
                        utcSixMonthsAgo.toInstant().toEpochMilli(),
                        utcNow.toInstant().toEpochMilli()
                ), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());
        // No access to that user
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /**
     * This test verifies whether the endpoint returns the latest best fall risk assessment for a certain group
     * consisting of a single user when querying the current time period (endTime = current time). Moreover, the latest
     * best fall risk assessments are cached for five minutes. Hence, we can save a new best fall risk assessment for
     * the user under test. Then, when we request the latest best fall risk assessment for the group, the endpoint
     * should not return the new best fall risk assessment (yet). Instead, it should return the old best one since the
     * response was cached. After five minutes (or when a non-cached time period is requested), the endpoint should
     * return the new best fall risk assessment.
     */
    @Test
    void testGetLatestBestFallRiskAssessmentsForGroupWithCaching() throws IOException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testGetLatestBestFallRiskAssessmentsWithCachingUser").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        Floor floor = Floor.builder().name("FRP test floor").build();
        floorRepository.save(floor);

        Footstep firstFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 1))
                .time(LocalDateTime.now().minusSeconds(1).truncatedTo(ChronoUnit.MILLIS))
                .build();
        Footstep secondFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 2))
                .time(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();
        List<Footstep> footsteps = new ArrayList<>(Arrays.asList(firstFootstep, secondFootstep));
        footstepRepository.saveAll(footsteps);

        /* The time window that the user wearable link spans (is valid in). Equivalent to: the last week. */
        LocalDateTime uwlBeginTime = LocalDateTime.now()
                .minusDays(7)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        /* The creation times of three FRPs of which only the second two should be in the result set. */
        LocalDateTime notTheBestFrpCreationTime = LocalDateTime.now()
                .minusDays(5)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime bestFrpCreationTime = LocalDateTime.now()
                .minusDays(3)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime newBestFrpCreationTime = LocalDateTime.now()
                .minusDays(1)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = new UserWearableLink();
        userWearableLink.setBeginTime(uwlBeginTime);
        userWearableLink.setEndTime(uwlEndTime);
        userWearableLink.setUser(user);
        userWearableLink.setWearable(wearable);
        userWearableLinkRepository.save(userWearableLink);

        /* These fall risk profiles all fall within the UWL's time window (last week) and the time window in which we
        call the endpoint (last week). However, only the second and third FRP are the best FRPs for the user.
        The endpoint should return the second FRP because it is the latest best FRP within the last five minutes
        (the cache time).
         */
        FallRiskProfile notTheBestFallRiskProfile = FallRiskProfile.builder()
                .wearable(wearable)
                .floor(floor)
                .creationTime(notTheBestFrpCreationTime)
                .beginTime(notTheBestFrpCreationTime).endTime(notTheBestFrpCreationTime.plusMinutes(2))
                .walkingSpeed(1.0).stepLength(2.0).stepFrequency(2.0).rmsVerticalAcceleration(2.0).build();

        FallRiskProfile bestFallRiskProfile = FallRiskProfile.builder()
                .wearable(wearable)
                .floor(floor)
                .creationTime(bestFrpCreationTime)
                .beginTime(bestFrpCreationTime).endTime(bestFrpCreationTime.plusMinutes(2))
                .walkingSpeed(2.0).stepLength(4.0).stepFrequency(4.0).rmsVerticalAcceleration(4.0).build();

        FallRiskProfile newBestFallRiskProfile = FallRiskProfile.builder()
                .wearable(wearable)
                .floor(floor)
                .creationTime(newBestFrpCreationTime)
                .beginTime(newBestFrpCreationTime).endTime(newBestFrpCreationTime.plusMinutes(2))
                .walkingSpeed(3.0).stepLength(6.0).stepFrequency(6.0).rmsVerticalAcceleration(6.0).build();

        fallRiskProfileRepository.saveAll(List.of(notTheBestFallRiskProfile, bestFallRiskProfile));

        // when

        // the end time of the request is 7 minutes before the end time of the UWL (the current time)
        // this means that the response should be cached for the time period [now - 10 minutes, now - 5 minutes]
        // since the cache key is the last 5 minutes of the end time of the request time window
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        String.format(
                                "/analyses/fall-risk/latest/groups/%d?begin=%d&end=%d",
                                group.getId(),
                                uwlBeginTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                uwlEndTime.minusMinutes(7).toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then
        List<LatestFallRiskProfileAssessment> fallRiskAssessments = List.of(
                mapper.readValue(
                        response.getBody(),
                        LatestFallRiskProfileAssessment[].class
                )
        );

        assertNotNull(fallRiskAssessments);
        assertEquals(1, fallRiskAssessments.size());

        assertEquals(bestFallRiskProfile.getEndTime(), fallRiskAssessments.get(0).getTime());

        // and given a new best FRP that was created within the UWL time period (one day before the UWL ends)
        fallRiskProfileRepository.save(newBestFallRiskProfile);

        // when

        // the end time of the request is 7 minutes before the end time of the UWL (the current time)
        // this means that a cached response should be returned since the requested time period is
        // [now - 10 minutes, now - 5 minutes] for which we have cached the response with the previous best FRP
        HttpEntity<String> cachedResponse = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        String.format(
                                "/analyses/fall-risk/latest/groups/%d?begin=%d&end=%d",
                                group.getId(),
                                uwlBeginTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                uwlEndTime.minusMinutes(7).toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );

        // the end time of the request is the end time of the UWL (the current time)
        // this means that a new (non-cached) response should be returned since the requested time period is
        // [now - 5 minutes, now] for which we have not cached a response yet
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        String.format(
                                "/analyses/fall-risk/latest/groups/%d?begin=%d&end=%d",
                                group.getId(),
                                uwlBeginTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then

        // we have a list of cached latest fall risk profile assessments for the group
        List<LatestFallRiskProfileAssessment> cachedFallRiskAssessments = List.of(
                mapper.readValue(
                        cachedResponse.getBody(),
                        LatestFallRiskProfileAssessment[].class
                )
        );

        // and a list of latest (non-cached) fall risk profile assessments for the group
        fallRiskAssessments = List.of(
                mapper.readValue(
                        response.getBody(),
                        LatestFallRiskProfileAssessment[].class
                )
        );

        assertNotNull(cachedFallRiskAssessments);
        assertEquals(1, cachedFallRiskAssessments.size());

        assertNotNull(fallRiskAssessments);
        assertEquals(1, fallRiskAssessments.size());

        // then the cached response contains the previous best FRP
        assertEquals(bestFallRiskProfile.getEndTime(), cachedFallRiskAssessments.get(0).getTime());

        // and the non-cached response contains the new best FRP, since the requested time window was not cached (yet)
        assertEquals(newBestFallRiskProfile.getEndTime(), fallRiskAssessments.get(0).getTime());
    }

    /**
     * Same as the previous test in that we request the fall risk assessments for a certain user within a time window.
     * However, for the given FRPs on which these assessments would be based, we hide one FRP. We verify that the
     * endpoint does not include a fall risk assessment into its result set of which the underlying FRP is hidden.
     */
    @Test
    void testIgnoreHiddenFallRiskAssessmentsForUserWithinTimeWindow() throws IOException {
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserAuthIgnoreHiddenFallRisk").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        Floor floor = Floor.builder().name("FRP test floor").build();
        floorRepository.save(floor);

        Footstep firstFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 1))
                .time(LocalDateTime.now().minusSeconds(1).truncatedTo(ChronoUnit.MILLIS))
                .build();
        Footstep secondFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 2))
                .time(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();
        List<Footstep> footsteps = new ArrayList<>(Arrays.asList(firstFootstep, secondFootstep));
        footstepRepository.saveAll(footsteps);

        /* The time window that the user wearable link spans (is valid in). */
        LocalDateTime uwlBeginTime = LocalDateTime.now()
                .minusDays(2)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        /* The creation times of the FRPs of which only the second two should be in the result set. */
        LocalDateTime frpCreationTime = LocalDateTime.now()
                .minusDays(1)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = new UserWearableLink();
        userWearableLink.setBeginTime(uwlBeginTime);
        userWearableLink.setEndTime(uwlEndTime);
        userWearableLink.setUser(user);
        userWearableLink.setWearable(wearable);
        userWearableLinkRepository.save(userWearableLink);

        /* We'll create fall risk profiles and save them, although normally the FRPs are inserted by an AWS Lambda. */

        /* The first FRP falls within the user wearable link time window and should be part of the result set. */
        FallRiskProfile matchingFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getEndTime())
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).build();

        /* The second FRP falls within the user wearable link time window and should normally be part of the result set.
         * However, we set its 'hidden' marker field to true so that it will (should) not be part of the result set. */
        FallRiskProfile nonMatchingFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime().plusSeconds(1L)).endTime(userWearableLink.getEndTime())
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).hidden(true).build();

        fallRiskProfileRepository.save(matchingFRP);
        fallRiskProfileRepository.save(nonMatchingFRP);

        ZonedDateTime utcNow = LocalDateTime.now().atZone(ZoneOffset.UTC);
        ZonedDateTime utcSixMonthsAgo = utcNow.minusMonths(6);

        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() +
                        String.format(
                                "?begin=%d&end=%d",
                                utcSixMonthsAgo.toInstant().toEpochMilli(),
                                utcNow.toInstant().toEpochMilli()
                        ), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());

        List<FallRiskAssessmentModel> fallRiskAssessments =
                Arrays.asList(mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class));

        assertNotNull(fallRiskAssessments);
        assertEquals(1, fallRiskAssessments.size());

        List<FallRiskProfile> retrievedFRPs = fallRiskAssessments.stream()
                .map(FallRiskAssessmentModel::getFallRiskProfile)
                .toList();

        /* We ignore the wearable, floor fields in the fall risk profiles returned here as it is not serialized.
        Actually, AssertJ should be able to check wearable.tenant specifically but for some reason this was not working
        while writing this test. */
        Assertions.assertThat(retrievedFRPs.get(0))
                .usingRecursiveComparison()
                .ignoringFields("wearable")
                .ignoringFields("floor")
                .ignoringFields("rmsVerticalAcceleration")
                .ignoringFields("notes")
                .isEqualTo(matchingFRP);

        /* Check wearable id specifically as it was ignored in the previous assertions. */
        assertEquals(retrievedFRPs.get(0).getWearable().getId(), matchingFRP.getWearable().getId());

        /* Check floor id specifically as it was ignored in the previous assertions. */
        assertEquals(retrievedFRPs.get(0).getFloor().getId(), matchingFRP.getFloor().getId());
    }

    /**
     * In this test, we test the exact opposite of the previous test. That is, we verify that the fall risk assessments
     * returned by the endpoint that we test here are only based on hidden FRPs.
     */
    @Test
    void testGetHiddenFallRiskAssessmentsIncludedForUser() throws IOException {
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserAuthForHiddenFallRisk").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        Floor floor = Floor.builder().name("FRP test floor").build();
        floorRepository.save(floor);

        Footstep firstFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 1))
                .time(LocalDateTime.now().minusSeconds(1).truncatedTo(ChronoUnit.MILLIS))
                .build();
        Footstep secondFootstep = Footstep.builder()
                .wearable(wearable)
                .position(new Position(1, 2))
                .time(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();
        List<Footstep> footsteps = new ArrayList<>(Arrays.asList(firstFootstep, secondFootstep));
        footstepRepository.saveAll(footsteps);

        /* The time window that the user wearable link spans (is valid in). */
        LocalDateTime uwlBeginTime = LocalDateTime.now()
                .minusDays(2)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        /* The creation times of the FRPs of which only the second two should be in the result set. */
        LocalDateTime frpCreationTime = LocalDateTime.now()
                .minusDays(1)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = new UserWearableLink();
        userWearableLink.setBeginTime(uwlBeginTime);
        userWearableLink.setEndTime(uwlEndTime);
        userWearableLink.setUser(user);
        userWearableLink.setWearable(wearable);
        userWearableLinkRepository.save(userWearableLink);

        /* We'll create fall risk profiles and save them, although normally the FRPs are inserted by an AWS Lambda. */

        /* The first FRP for the user is hidden and should be part of (included in) the result set. */
        FallRiskProfile matchingFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getEndTime())
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).hidden(true).build();

        /* The second FRP for the user is not hidden and should also be part of the result set. */
        FallRiskProfile secondMatchingFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime().plusSeconds(1L)).endTime(userWearableLink.getEndTime())
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).build();

        fallRiskProfileRepository.save(matchingFRP);
        fallRiskProfileRepository.save(secondMatchingFRP);

        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/include-hidden/users/" + user.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        mapper.registerModule(new GeoModule());

        List<FallRiskAssessmentModel> fallRiskAssessments =
                Arrays.asList(mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class));

        assertNotNull(fallRiskAssessments);
        assertEquals(2, fallRiskAssessments.size());

        List<FallRiskProfile> retrievedFRPs = fallRiskAssessments.stream()
                .map(FallRiskAssessmentModel::getFallRiskProfile)
                .toList();

        /* We ignore the wearable field in the fall risk profiles returned here as it is not serialized.
        Actually, AssertJ should be able to check wearable.tenant specifically but for some reason this was not working
        while writing this test. */
        Assertions.assertThat(retrievedFRPs.get(0))
                .usingRecursiveComparison()
                .ignoringFields("wearable")
                .ignoringFields("floor")
                .ignoringFields("rmsVerticalAcceleration")
                .ignoringFields("notes")
                .isEqualTo(matchingFRP);
        Assertions.assertThat(retrievedFRPs.get(1))
                .usingRecursiveComparison()
                .ignoringFields("wearable")
                .ignoringFields("floor")
                .ignoringFields("rmsVerticalAcceleration")
                .ignoringFields("notes")
                .isEqualTo(secondMatchingFRP);

        /* Check wearable id's specifically as they were ignored in the previous assertions. */
        assertEquals(retrievedFRPs.get(0).getWearable().getId(), matchingFRP.getWearable().getId());
        assertEquals(retrievedFRPs.get(1).getWearable().getId(), secondMatchingFRP.getWearable().getId());

        /* Check floor id's specifically as they were ignored in the previous assertions. */
        assertEquals(retrievedFRPs.get(0).getFloor().getId(), matchingFRP.getFloor().getId());
        assertEquals(retrievedFRPs.get(1).getFloor().getId(), secondMatchingFRP.getFloor().getId());
    }

    /**
     * TODO.
     */
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
