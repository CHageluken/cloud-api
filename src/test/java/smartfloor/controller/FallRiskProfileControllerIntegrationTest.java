package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
import smartfloor.domain.dto.FallRiskProfileNoteForm;
import smartfloor.domain.dto.FallRiskProfileRemovalForm;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.domain.entities.fall.risk.profile.FallRiskAssessmentModel;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileNote;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemoval;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemovalReason;
import smartfloor.domain.entities.fall.risk.profile.FallRiskScoreAssessment;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.FallRiskProfileNoteRepository;
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

class FallRiskProfileControllerIntegrationTest extends IntegrationTestBase {

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
    @Autowired
    FallRiskProfileNoteRepository fallRiskProfileNoteRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create or get an existing test wearable.
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
     * Create or get an existing test wearable group.
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
     * Create or get an existing test group with a list of provided users.
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

    /**
     * Create or get an existing test user with provided authId and tenant.
     */
    User getGroupUser(String authId, Tenant t) {
        return userRepository.findByAuthId(authId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .authId(authId)
                                .tenant(t)
                                .build())
                );
    }

    /**
     * Create or get an existing test floor with provided name.
     */
    Floor getTestFloor(String floorName) {
        return floorRepository.findByName(floorName)
                .orElseGet(() -> floorRepository.save(
                        Floor.builder()
                                .name(floorName)
                                .build()
                ));
    }

    @BeforeEach
    void setup() {
        fallRiskProfileRepository.deleteAll();
        fallRiskProfileNoteRepository.deleteAll();
    }


    @Test
    void testSoftDeleteFallRiskProfile() {
        Tenant tenant = getTestTenant();

        assertTrue(fallRiskProfileRepository.findAll().isEmpty());
        assertTrue(fallRiskProfileRepository.findAllRemoved().isEmpty());
        assertTrue(fallRiskRemovalRepository.findAll().isEmpty());

        User user = User.builder().tenant(tenant).authId("testUserAuthForRemoveFallRisk").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        Floor floor = getTestFloor("FRP test floor");

        /* The time window that the user wearable link spans (is valid in). */
        LocalDateTime uwlBeginTime =
                LocalDateTime.now().minusDays(2).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        LocalDateTime frpCreationTime =
                LocalDateTime.now().minusDays(1).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(uwlBeginTime)
                .endTime(uwlEndTime)
                .wearable(wearable)
                .user(user)
                .build();
        userWearableLinkRepository.save(userWearableLink);

        FallRiskProfile toBeRemovedFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getBeginTime().plusSeconds(4L))
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0)
                .build();

        toBeRemovedFRP = fallRiskProfileRepository.save(toBeRemovedFRP);

        // Test that the FRP is not deleted by default
        List<FallRiskProfileRemoval> fallRiskProfileRemoval = fallRiskRemovalRepository.findAll();
        assertTrue(fallRiskProfileRemoval.isEmpty());

        String specificationOther = "Test";
        FallRiskProfileRemovalForm firstRemoval = FallRiskProfileRemovalForm.builder()
                .reasonForRemoval(FallRiskProfileRemovalReason.OTHER)
                .specificationOther(specificationOther)
                .build();

        // create body for removeFRP
        HttpEntity<FallRiskProfileRemovalForm> entity = new HttpEntity<>(firstRemoval, TestUtils.defaultHttpHeaders());
        Long fallRiskProfileId = toBeRemovedFRP.getId();

        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/v1/fall-risk-profiles/" + fallRiskProfileId, getPort()),
                HttpMethod.DELETE, entity, String.class
        );

        Optional<FallRiskProfile> removedFRP = fallRiskProfileRepository.findById(fallRiskProfileId);
        assertFalse(removedFRP.isPresent());

        FallRiskProfile secondFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime().plusSeconds(5L)).endTime(userWearableLink.getEndTime())
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0).build();

        fallRiskProfileRepository.save(secondFRP);

        List<FallRiskProfile> notRemovedFRPs = fallRiskProfileRepository.findAll();
        List<FallRiskProfile> removedFRPs = fallRiskProfileRepository.findAllRemoved();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, notRemovedFRPs.size());
        assertEquals(1, removedFRPs.size());
        /*Also assert all fields of the form are persisted properly*/
        FallRiskProfileRemoval removal = fallRiskRemovalRepository.findAll().get(0);
        assertNotNull(removal.getDeletedAt());
        assertEquals(getTestUser().getId(), removal.getDeletedBy().getId());
        assertEquals(FallRiskProfileRemovalReason.OTHER, removal.getReasonForRemoval());
        assertEquals(specificationOther, removal.getSpecificationOther());
    }

    @Test
    void testSoftDeleteNonExistentFallRiskProfile() {
        Tenant tenant = getTestTenant();

        assertTrue(fallRiskProfileRepository.findAll().isEmpty());
        assertTrue(fallRiskProfileRepository.findAllRemoved().isEmpty());
        assertTrue(fallRiskRemovalRepository.findAll().isEmpty());

        User user = User.builder().tenant(tenant).authId("testUserAuthForRemoveNonExistentFallRisk").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        Floor floor = getTestFloor("FRP test floor");

        /* The time window that the user wearable link spans (is valid in). */
        LocalDateTime uwlBeginTime =
                LocalDateTime.now().minusDays(2).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        LocalDateTime frpCreationTime =
                LocalDateTime.now().minusDays(1).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(uwlBeginTime)
                .endTime(uwlEndTime)
                .wearable(wearable)
                .user(user)
                .build();
        userWearableLinkRepository.save(userWearableLink);

        FallRiskProfile toBeRemovedFRP = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getBeginTime().plusSeconds(4L))
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0)
                .build();

        toBeRemovedFRP = fallRiskProfileRepository.save(toBeRemovedFRP);

        // Test that the FRP is not deleted by default
        List<FallRiskProfileRemoval> fallRiskProfileRemoval = fallRiskRemovalRepository.findAll();
        assertTrue(fallRiskProfileRemoval.isEmpty());

        FallRiskProfileRemovalForm firstRemoval = FallRiskProfileRemovalForm.builder()
                .reasonForRemoval(FallRiskProfileRemovalReason.SENSOR)
                .build();

        // create body for removeFRP
        HttpEntity<FallRiskProfileRemovalForm> entity = new HttpEntity<>(firstRemoval, TestUtils.defaultHttpHeaders());
        Long fallRiskProfileId = toBeRemovedFRP.getId();

        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/v1/fall-risk-profiles/" + fallRiskProfileId + 1L, getPort()),
                HttpMethod.DELETE, entity, String.class
        );

        List<FallRiskProfile> notRemovedFRPs = fallRiskProfileRepository.findAll();

        assertEquals(1, notRemovedFRPs.size());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testFallRiskProfileNoteCreate() throws JsonProcessingException {
        // given: An FRP, made during a valid UWL, on a specific floor, etc.
        Tenant tenant = getTestTenant();
        assertTrue(fallRiskProfileRepository.findAll().isEmpty());
        assertTrue(fallRiskProfileNoteRepository.findAll().isEmpty());
        User user = getGroupUser("testUserForFrpNote", tenant);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        Floor floor = getTestFloor("FRP test floor");
        LocalDateTime uwlBeginTime =
                LocalDateTime.now().minusDays(2).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        LocalDateTime frpCreationTime =
                LocalDateTime.now().minusDays(1).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(uwlBeginTime)
                .endTime(uwlEndTime)
                .wearable(wearable)
                .user(user)
                .build();
        userWearableLinkRepository.save(userWearableLink);
        FallRiskProfile frp = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getBeginTime().plusSeconds(4L))
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0)
                .build();
        frp = fallRiskProfileRepository.save(frp);

        // when: We fetch the FRP (assessment)
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() +
                        String.format(
                                "?begin=%d&end=%d",
                                uwlBeginTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                uwlEndTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ), getPort()),
                HttpMethod.GET, entity, String.class
        );

        List<FallRiskAssessmentModel> fallRiskAssessments =
                Arrays.asList(mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class));

        assertNotNull(fallRiskAssessments);
        assertEquals(1, fallRiskAssessments.size());

        FallRiskProfile retrievedFRP = fallRiskAssessments.stream()
                .map(FallRiskAssessmentModel::getFallRiskProfile)
                .toList().get(0);

        // then: The FRP should have an empty note
        assertNull(retrievedFRP.getFallRiskProfileNote());
        // then given: We add a note to the FRP
        FallRiskProfileNoteForm form = FallRiskProfileNoteForm.builder()
                .noteValue("Test note")
                .build();
        entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/v1/fall-risk-profiles/" + retrievedFRP.getId(), getPort()),
                HttpMethod.PUT, entity, String.class
        );
        retrievedFRP = mapper.readValue(response.getBody(), FallRiskProfile.class);
        // We check that the update endpoint returns the FRP with a note
        assertNotNull(retrievedFRP.getFallRiskProfileNote());
        assertEquals("Test note", retrievedFRP.getFallRiskProfileNote().getValue());
        // then when: We fetch the FRP Assessment and check the note again
        entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() +
                        String.format(
                                "?begin=%d&end=%d",
                                uwlBeginTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                uwlEndTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ), getPort()),
                HttpMethod.GET, entity, String.class
        );

        fallRiskAssessments =
                Arrays.asList(mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class));

        assertNotNull(fallRiskAssessments);
        assertEquals(1, fallRiskAssessments.size());

        retrievedFRP = fallRiskAssessments.stream()
                .map(FallRiskAssessmentModel::getFallRiskProfile)
                .toList().get(0);

        // then: The FRP should have a non-empty note
        assertEquals(1, fallRiskProfileNoteRepository.findAll().size());
        assertNotNull(retrievedFRP.getFallRiskProfileNote());
        FallRiskProfileNote note = retrievedFRP.getFallRiskProfileNote();
        assertEquals("Test note", note.getValue());
        assertEquals(getTestUser().id, note.getCreatedBy().id);
        // The note must have a creation time, but no update time (since the note has not been updated yet)
        assertNotNull(note.getCreatedAt());
        assertNull(note.getUpdatedAt());
    }

    @Test
    void testFallRiskProfileNoteUpdate() throws JsonProcessingException {
        // given: An FRP with a note, made during a valid UWL, on a specific floor, etc.
        Tenant tenant = getTestTenant();
        assertTrue(fallRiskProfileRepository.findAll().isEmpty());
        assertTrue(fallRiskProfileNoteRepository.findAll().isEmpty());
        User user = getGroupUser("testUserForFrpNote", tenant);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        Floor floor = getTestFloor("FRP test floor");
        LocalDateTime uwlBeginTime =
                LocalDateTime.now().minusDays(2).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        LocalDateTime frpCreationTime =
                LocalDateTime.now().minusDays(1).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(uwlBeginTime)
                .endTime(uwlEndTime)
                .wearable(wearable)
                .user(user)
                .build();
        userWearableLinkRepository.save(userWearableLink);
        FallRiskProfile frp = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getBeginTime().plusSeconds(4L))
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0)
                .build();
        frp = fallRiskProfileRepository.save(frp);
        FallRiskProfileNote note = FallRiskProfileNote.builder()
                .value("Test note")
                .fallRiskProfile(frp)
                .createdBy(getTestUser())
                .build();
        fallRiskProfileNoteRepository.save(note);

        // then given: We try to add a new note
        FallRiskProfileNoteForm form = FallRiskProfileNoteForm.builder()
                .noteValue("Test note updated")
                .build();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/v1/fall-risk-profiles/" + frp.getId(), getPort()),
                HttpMethod.PUT, entity, String.class
        );
        frp = mapper.readValue(response.getBody(), FallRiskProfile.class);
        // then: We check that the update endpoint returns the FRP with the updated note
        assertEquals(1, fallRiskProfileNoteRepository.findAll().size());
        assertNotNull(frp.getFallRiskProfileNote());
        note = frp.getFallRiskProfileNote();
        assertEquals("Test note updated", note.getValue());
        assertEquals(getTestUser().id, note.getCreatedBy().id);
        // The note must have both creation and update time
        assertNotNull(note.getCreatedAt());
        assertNotNull(note.getUpdatedAt());
        // No new notes should have been created
        assertEquals(1, fallRiskProfileNoteRepository.findAll().size());
    }

    @Test
    void testFallRiskProfileNoteDelete() throws JsonProcessingException {
        // given: An FRP with a note, made during a valid UWL, on a specific floor, etc.
        Tenant tenant = getTestTenant();
        assertTrue(fallRiskProfileRepository.findAll().isEmpty());
        assertTrue(fallRiskProfileNoteRepository.findAll().isEmpty());
        User user = getGroupUser("testUserForFrpNote", tenant);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        Floor floor = getTestFloor("FRP test floor");
        LocalDateTime uwlBeginTime =
                LocalDateTime.now().minusDays(2).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime uwlEndTime =
                LocalDateTime.now().atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);

        LocalDateTime frpCreationTime =
                LocalDateTime.now().minusDays(1).atZone(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        UserWearableLink userWearableLink = UserWearableLink.builder()
                .beginTime(uwlBeginTime)
                .endTime(uwlEndTime)
                .wearable(wearable)
                .user(user)
                .build();
        userWearableLinkRepository.save(userWearableLink);
        FallRiskProfile frp = FallRiskProfile.builder()
                .wearable(wearable).floor(floor)
                .creationTime(frpCreationTime)
                .beginTime(userWearableLink.getBeginTime()).endTime(userWearableLink.getBeginTime().plusSeconds(4L))
                .walkingSpeed(5.0).stepLength(6.0).stepFrequency(7.0).rmsVerticalAcceleration(8.0)
                .build();
        frp = fallRiskProfileRepository.save(frp);
        FallRiskProfileNote note = FallRiskProfileNote.builder()
                .value("Test note")
                .fallRiskProfile(frp)
                .createdBy(getTestUser())
                .build();
        fallRiskProfileNoteRepository.save(note);

        // then given: We update the note with an empty string
        FallRiskProfileNoteForm form = FallRiskProfileNoteForm.builder()
                .noteValue("")
                .build();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/v1/fall-risk-profiles/" + frp.getId(), getPort()),
                HttpMethod.PUT, entity, String.class
        );
        frp = mapper.readValue(response.getBody(), FallRiskProfile.class);
        // then: We check that the update endpoint returns the FRP with a null note
        assertNull(frp.getFallRiskProfileNote());
        // The notes repository should be empty
        assertEquals(0, fallRiskProfileNoteRepository.findAll().size());
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
