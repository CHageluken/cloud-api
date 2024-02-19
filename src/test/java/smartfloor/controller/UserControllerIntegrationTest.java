package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.assertj.core.api.Assertions;
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
import smartfloor.domain.UserMeasurementType;
import smartfloor.domain.UserType;
import smartfloor.domain.dto.CreateWearableUserForm;
import smartfloor.domain.dto.UpdateWearableUserForm;
import smartfloor.domain.dto.user.measurements.FallIncidentDetails;
import smartfloor.domain.dto.user.measurements.PomaMeasurementDetails;
import smartfloor.domain.dto.user.measurements.UserMeasurementForm;
import smartfloor.domain.entities.Application;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.entities.UserMeasurement;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.user.info.history.GenderConstants;
import smartfloor.domain.entities.user.info.history.UserInfoHistory;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.ApplicationRepository;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserInfoHistoryRepository;
import smartfloor.repository.jpa.UserMeasurementRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.util.TestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerIntegrationTest extends IntegrationTestBase {

    private static final String USERS_REST_ENDPOINT = "/users";
    private static final String COGNITO_USERS_REST_ENDPOINT = "/users/cognito";
    private static final String USER_APPLICATIONS_REST_ENDPOINT = "/users/accessible-applications";
    private static final String GROUPS_REST_ENDPOINT = "/groups";
    private static final int DEFAULT_USER_LIMIT = 1;
    private static final long TENANT_WITH_USER_LIMIT_TENANT_ID = 3L;

    @Autowired
    UserRepository userRepository;
    @Autowired
    UserInfoHistoryRepository userInfoHistoryRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    UserMeasurementRepository userMeasurementRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    CompositeUserRepository compositeUserRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    private User authenticatedUser;
    private User authenticatedUserWithLimit;

    /**
     * TODO.
     */
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
        userMeasurementRepository.deleteAll();
        userRepository.deleteAll();
        authenticatedUser = getTestUser();

        AccessScopeContext.INSTANCE.setTenantId(TENANT_WITH_USER_LIMIT_TENANT_ID);
        userMeasurementRepository.deleteAll();
        userRepository.deleteAll();
        User limitedAuthenticatedUser = User
                .builder()
                .authId(TestUtils.TEST_USER_AUTH_ID)
                .tenant(getTestTenantWithUserLimit())
                .build();
        authenticatedUserWithLimit = userRepository.save(limitedAuthenticatedUser);

        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId()); // set default tenant context again
    }

    /**
     * TODO.
     */
    @Test
    void testGetUserDetails() throws IOException {
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + "me", getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        User userFromResponse = mapper.readValue(response.getBody(), User.class);
        /* TODO: We ignore a couple of fields in the assertion below
            because lazily initializing these collections will fail before serialization.
            Should look into fixing this without eagerly fetching the fields.
            Possibly we can still use the getters for these fields on the User object and just have separate
            REST endpoints to fetch fields like this for a user.
         */
        // then we expect the user to be the (authenticated) test user, we ignore the tenant as it is not serialized on
        // the User side
        Assertions.assertThat(userFromResponse)
                .usingRecursiveComparison()
                .ignoringFields("tenant")
                .ignoringFields("userWearableLinks")
                .ignoringFields("sessions")
                .ignoringFields("createdSessions")
                .ignoringFields("groups")
                .ignoringFields("measurements")
                .isEqualTo(authenticatedUser);

        // then when: We call the endpoint, authenticating as a composite user
        String compositeUserAuthId = "testGetUserProfileCU";
        HttpHeaders cuHeaders = setCUWithUserAndGetHTTPHeaders(null, compositeUserAuthId);
        CompositeUser compositeUser = compositeUserRepository.findByAuthId(compositeUserAuthId).get();
        entity = new HttpEntity<>(null, cuHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + "me", getPort()),
                HttpMethod.GET, entity, String.class
        );
        CompositeUser compositeUserFromResponse = mapper.readValue(response.getBody(), CompositeUser.class);
        // then
        assertEquals(compositeUserFromResponse, compositeUser);
    }

    /**
     * TODO.
     */
    @Test
    void testGetUsers() throws IOException {
        User user = new User();
        user.setTenant(getTestTenant());
        user.setAuthId("testAuthGet");
        userRepository.save(user);

        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<User> usersFromResponse = mapper.readValue(response.getBody(), List.class);
        assertNotEquals(0, usersFromResponse.size());
    }

    /**
     * TODO.
     */
    @Test
    void testGetUsersForCU() throws JsonProcessingException {
        // given: A CU with access to 2/3 regular users (sub-users), and another CU with access to the remaining reg.
        // user. We also create some group managers to evaluate access to them.
        CompositeUser compositeUser1 = CompositeUser.builder()
                .authId("testGetUsersForCU1")
                .build();
        CompositeUser compositeUser2 = CompositeUser.builder()
                .authId("testGetUsersForCU2")
                .build();
        compositeUser1 = compositeUserRepository.save(compositeUser1);
        compositeUser2 = compositeUserRepository.save(compositeUser2);

        User user1 = User.builder()
                .tenant(getTestTenant())
                .authId("testGetUserAuth1")
                .compositeUser(compositeUser1)
                .build();
        User user2 = User.builder()
                .tenant(getTestTenant())
                .authId("testGetUserAuth2")
                .compositeUser(compositeUser1)
                .build();
        User user3 = User.builder()
                .tenant(getTestTenant())
                .authId("testGetUserAuth3")
                .compositeUser(compositeUser2)
                .build();
        User gm1 = User.builder()
                .tenant(getTestTenant())
                .authId("testGetUserAuth4")
                .build();
        User gm2 = User.builder()
                .tenant(getTestTenant())
                .authId("testGetUserAuth5")
                .build();
        userRepository.saveAll(List.of(user1, user2, user3, gm1, gm2));

        List<User> firstCUSubUsers = List.of(user1, user2);
        List<User> secondCUSubUsers = List.of(user3);

        Group g1 = Group.builder()
                .tenant(getTestTenant())
                .managers(List.of(gm1))
                .users(List.of(user1, user2))
                .name("testGetUserGroup1")
                .build();
        Group g2 = Group.builder()
                .tenant(getTestTenant())
                .managers(List.of(gm2))
                .users(List.of(user3))
                .name("testGetUserGroup2")
                .build();
        groupRepository.saveAll(List.of(g1, g2));

        // when: The endpoint for listing users is called by CU1
        HttpHeaders firstCUHeaders = setCUWithUserAndGetHTTPHeaders(null, compositeUser1.getAuthId());

        HttpEntity<String> entity = new HttpEntity<>(null, firstCUHeaders);
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<User> usersFromResponse = mapper.readValue(response.getBody(), new TypeReference<List<User>>() {
        });
        List<User> usersVisibleInDB = userRepository.findAll();

        // then: We expect only two users (the sub-users) to be visible.
        assertEquals(firstCUSubUsers.size(), usersFromResponse.size());
        assertEquals(firstCUSubUsers.size(), usersVisibleInDB.size());

        // when: The endpoint for listing users is called by CU2
        HttpHeaders secondCUHeaders = setCUWithUserAndGetHTTPHeaders(null, compositeUser2.getAuthId());

        entity = new HttpEntity<>(null, secondCUHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        usersFromResponse = mapper.readValue(response.getBody(), new TypeReference<List<User>>() {
        });
        usersVisibleInDB = userRepository.findAll();

        // then: We expect only one user to be visible.
        assertEquals(secondCUSubUsers.size(), usersFromResponse.size());
        assertEquals(secondCUSubUsers.size(), usersVisibleInDB.size());
        // We can also make sure that it is user3
        assertEquals(user3.getId(), usersFromResponse.get(0).getId());
    }

    /**
     * TODO.
     */
    @Test
    void testGetUser() throws IOException {
        Wearable wearable = new Wearable();
        wearable.setId("testWearable");

        User user = new User();
        user.setTenant(getTestTenant());
        user.setAuthId("testAuthGetOne");
        user = userRepository.save(user);

        HttpEntity<String> entity = new HttpEntity<>(user.getId().toString(), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId(), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        User userFromResponse = mapper.readValue(response.getBody(), User.class);
        assertEquals(user.getId(), userFromResponse.getId());
    }

    /**
     * TODO.
     */
    @Test
    void testDeleteUser() {
        User user = new User();
        user.setTenant(getTestTenant());
        user.setAuthId("testAuthDelete");
        user = userRepository.save(user);

        int amountOfUsers = userRepository.findAll().size();
        HttpEntity<String> entity = new HttpEntity<>(user.getId().toString(), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId(), getPort()),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertEquals(amountOfUsers - 1, userRepository.findAll().size());
    }

    /**
     * TODO.
     */
    @Test
    void testUpdateArchivedUser() throws JsonProcessingException {
        // given: archived user
        User user = User.builder()
                .tenant(getTestTenant())
                .authId("testUpdateArchivedUser")
                .archivedAt(LocalDateTime.now().minusWeeks(1)) // Subtract a week so we can unarchive the user later
                .build();
        user = userRepository.save(user);

        UserInfo info = UserInfo.builder()
                .age(50)
                .height(170)
                .build();
        UpdateWearableUserForm updateWearableUserForm = UpdateWearableUserForm.builder().info(info).build();

        // when
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(updateWearableUserForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId(), getPort()),
                HttpMethod.PUT,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        // then when: unarchive the user and try to update them again
        HttpEntity<String> entity1 = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/unarchive", getPort()),
                HttpMethod.PATCH,
                entity1,
                String.class
        );
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId(), getPort()),
                HttpMethod.PUT,
                entity,
                String.class
        );

        // then
        User userFromResponse = mapper.readValue(response.getBody(), User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(info, userFromResponse.getInfo());
    }

    /**
     * TODO.
     */
    void testCreateInitialUserInfoGender(String gender) throws JsonProcessingException {
        // given
        User user = new User();
        user.setTenant(getTestTenant());
        user.setAuthId("testUpdateUserInfo");
        user = userRepository.save(user);

        UserInfo updatedInfo = UserInfo.builder().age(65).height(180).gender(gender).build();
        UpdateWearableUserForm updateWearableUserForm = UpdateWearableUserForm.builder().info(updatedInfo).build();

        // when
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(updateWearableUserForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId(), getPort()),
                HttpMethod.PUT,
                entity,
                String.class
        );

        // then
        User userFromResponse = mapper.readValue(response.getBody(), User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UserInfo userInfoResponse = userFromResponse.getInfo();
        assertEquals(updatedInfo, userInfoResponse);
        assertTrue(GenderConstants.SUPPORTED_GENDERS.contains(userInfoResponse.getGender()));
    }

    /**
     * TODO.
     */
    @Test
    void testCreateInitialUserInfo() throws JsonProcessingException {
        testCreateInitialUserInfoGender("m");
        setUp();
        testCreateInitialUserInfoGender("");
        setUp();
        testCreateInitialUserInfoGender("t");
        setUp();
        testCreateInitialUserInfoGender(null);
    }

    /**
     * TODO.
     */
    void testUpdateUserInfoWithPreviousInfoAvailableGender(String gender)
            throws JsonProcessingException, ExecutionException, InterruptedException {
        // given
        User user = new User();
        UserInfo info = UserInfo.builder().age(50).height(190).build();
        user.setTenant(getTestTenant());
        user.setAuthId("testUpdateUserInfo");

        user.setInfo(info);
        user = userRepository.save(user);

        UserInfo updatedInfo = UserInfo.builder().age(65).height(180).gender(gender).build();
        UpdateWearableUserForm updateWearableUserForm = UpdateWearableUserForm.builder().info(updatedInfo).build();

        // when
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(updateWearableUserForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId(), getPort()),
                HttpMethod.PUT,
                entity,
                String.class
        );

        // then
        User userFromResponse = mapper.readValue(response.getBody(), User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserInfo userInfoResponse = userFromResponse.getInfo();
        assertEquals(updatedInfo, userInfoResponse);
        assertTrue(GenderConstants.SUPPORTED_GENDERS.contains(userInfoResponse.getGender()));

        // and check that user info history records are kept correctly
        List<UserInfoHistory> userInfoHistories = userInfoHistoryRepository.findAllByUserIdOrderByEndTime(user.getId());
        assertEquals(1, userInfoHistories.size());
        assertEquals(info, userInfoHistories.get(0).getInfo());
    }

    /**
     * TODO.
     */
    @Test
    void testUpdateUserInfoWithPreviousInfoAvailable()
            throws JsonProcessingException, ExecutionException, InterruptedException {
        testUpdateUserInfoWithPreviousInfoAvailableGender("t");
        setUp();
        testUpdateUserInfoWithPreviousInfoAvailableGender("");
        setUp();
        testUpdateUserInfoWithPreviousInfoAvailableGender("m");
        setUp();
        testCreateInitialUserInfoGender(null);
    }

    /**
     * TODO.
     */
    @Test
    void testUpdateUserInfoWithSameInfo()
            throws JsonProcessingException, ExecutionException, InterruptedException {
        // given
        User user = new User();
        UserInfo info = UserInfo.builder().age(50).height(190).build();
        user.setTenant(getTestTenant());
        user.setAuthId("testUpdateUserInfo");
        user.setInfo(info);
        user = userRepository.save(user);

        UpdateWearableUserForm updateWearableUserForm = UpdateWearableUserForm.builder().info(info).build();

        // when
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(updateWearableUserForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId(), getPort()),
                HttpMethod.PUT,
                entity,
                String.class
        );

        // then
        User userFromResponse = mapper.readValue(response.getBody(), User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(info, userFromResponse.getInfo());

        // and check that no user info history record is created (as user info was identical)
        List<UserInfoHistory> userInfoHistories = userInfoHistoryRepository.findAllByUserIdOrderByEndTime(user.getId());
        assertEquals(0, userInfoHistories.size());
    }

    /**
     * Added as part of VIT-699: we test that concurrent user creation is handled correctly for the case where they are
     * added to the same group. This test failed during concurrent requests made to the users endpoint until pessimistic
     * write locking was implemented for the group update that made the new user a part of the group.
     */
    @Test
    void testConcurrentCreateWearableUserForIdenticalGroup() throws ExecutionException, InterruptedException {
        // create a group with the group repository that the users will be made members of
        Group group =
                Group.builder().tenant(Tenant.getDefaultTenant()).name("testConcurrentCreateWearableUser").build();
        groupRepository.save(group);

        // given a CreateWearableUserForm, create a user concurrently
        CreateWearableUserForm form = CreateWearableUserForm.builder()
                .userGroupId(group.getId())
                .build();

        // when we send create user requests concurrently
        final int AMOUNT_OF_USERS_TO_CREATE = 1;
        ExecutorService executor = Executors.newFixedThreadPool(AMOUNT_OF_USERS_TO_CREATE);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < AMOUNT_OF_USERS_TO_CREATE; i++) {
            futures.add(executor.submit(() -> {
                HttpEntity<String> entity =
                        new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
                return getRestTemplate().exchange(
                        TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                        HttpMethod.POST,
                        entity,
                        String.class
                );
            }));
        }

        // assert all responses have status 201 CREATED indicating that the concurrent requests were handled correctly
        for (Future<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get();
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }
    }

    /**
     * TODO.
     */
    @Test
    void testRecordPomaUserMeasurement() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);

        UserMeasurementForm form = UserMeasurementForm.builder()
                .type(UserMeasurementType.POMA)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(28.0)
                .build();

        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then
        UserMeasurement measurement = mapper.readValue(response.getBody(), UserMeasurement.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(authenticatedUser.getId(), measurement.getRecordedBy().getId());
        assertEquals(user.getId(), measurement.getUser().getId());
        assertEquals(form.getRecordedAt().truncatedTo(ChronoUnit.MILLIS), measurement.getRecordedAt());
        assertEquals(form.getType(), measurement.getType());
        assertEquals(form.getValue(), measurement.getValue());
    }

    /**
     * TODO.
     */
    @Test
    void testRecordUserMeasurementForArchivedUser() throws JsonProcessingException {
        // given
        User user = User.builder()
                .tenant(getTestTenant())
                .archivedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        UserMeasurementForm form = UserMeasurementForm.builder()
                .type(UserMeasurementType.POMA)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(28.0)
                .build();

        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /**
     * TODO.
     */
    @Test
    void testRecordValidPomaUserMeasurementDetails() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);

        PomaMeasurementDetails partialDetails = PomaMeasurementDetails.builder()
                .balanceTotal(16)
                .mobilityTotal(12)
                .build();

        UserMeasurementForm partialDetailsForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.POMA)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(28.0)
                .details(createDetailsFromPomaMeasurementDetails(partialDetails))
                .build();

        PomaMeasurementDetails fullDetails = PomaMeasurementDetails.builder()
                .sittingBalance(1)
                .arises(2)
                .attemptsToArise(1)
                .immediateStandingBalance(1)
                .standingBalance(2)
                .nudged(2)
                .eyesClosed(0)
                .turning360DegreesSteps(0)
                .turning360DegreesSteadiness(0)
                .sittingDown(1)
                .balanceTotal(10)
                .initiationOfGait(0)
                .stepLengthHeightRightPassesLeft(1)
                .stepLengthHeightRightClearsFloor(1)
                .stepLengthHeightLeftPassesRight(1)
                .stepLengthHeightLeftClearsFloor(1)
                .stepSymmetry(1)
                .stepContinuity(0)
                .path(2)
                .trunk(1)
                .walkingStance(1)
                .mobilityTotal(9)
                .build();

        UserMeasurementForm fullDetailsForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.POMA)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(19.0)
                .details(createDetailsFromPomaMeasurementDetails(fullDetails))
                .build();

        // when
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(partialDetailsForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // when
        entity = new HttpEntity<>(mapper.writeValueAsString(fullDetailsForm), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    /**
     * TODO.
     */
    @Test
    void testRecordInvalidPomaUserMeasurementDetails() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);

        PomaMeasurementDetails invalidValueSumDetails = PomaMeasurementDetails.builder()
                .balanceTotal(16)
                .mobilityTotal(12)
                .build();
        UserMeasurementForm invalidValueSumForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.POMA)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(
                        Integer.sum(
                                invalidValueSumDetails.getBalanceTotal(),
                                invalidValueSumDetails.getMobilityTotal()
                        ) - 1.0
                )
                .details(
                        createDetailsFromPomaMeasurementDetails(invalidValueSumDetails)
                )
                .build();

        PomaMeasurementDetails invalidBalanceSumDetails = PomaMeasurementDetails.builder()
                .arises(1)
                .balanceTotal(16)
                .mobilityTotal(12)
                .build();
        UserMeasurementForm invalidBalanceSumForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.POMA)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(
                        Integer.sum(
                                invalidBalanceSumDetails.getBalanceTotal(),
                                invalidBalanceSumDetails.getMobilityTotal()
                        ) * 1.0
                )
                .details(
                        createDetailsFromPomaMeasurementDetails(invalidBalanceSumDetails)
                )
                .build();

        // when
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(invalidValueSumForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // when
        entity = new HttpEntity<>(mapper.writeValueAsString(invalidBalanceSumForm), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Tests the creation of fall incidents with different values and causes.
     * There are generally two "valid" types of fall incidents:
     * 1. With a `value` of 0 and empty `details`. Such records indicate a no-fall measurement. It has no severity, and
     * no causes for a fall (since there is no fall). Those measurements are not displayed on the frontend.
     * 2. With a `value` >0 and fall `details`. The value indicates the severity of the fall, and the details contain
     * a `causes` field, which is a collection of strings. There must be at least 1 specified cause for a fall incident.
     */
    @Test
    void testRecordValidFallIncidentDetails() throws JsonProcessingException {
        // given: A fall incident form with missing details.
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);
        String validCause1 = "Bad eyesight";
        String validCause2 = "Impaired balance";

        // Any value >0 indicates that there has been a fall with varying severity. When there is a fall,
        // (valid) details for the incident are required.
        double valueThatRequiresDetails = 1;
        double valueThatDoesNotRequireDetails = 0;

        UserMeasurementForm validForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(valueThatDoesNotRequireDetails)
                .build();

        // when: We try recording a no-fall event without providing details
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(validForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then: Empty details are accepted when recording a no-fall event
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // then when: We try recording a fall incident with a valid fall cause
        FallIncidentDetails validDetails = FallIncidentDetails.builder()
                .causes(List.of(validCause1, validCause2))
                .build();

        validForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(valueThatRequiresDetails)
                .details(createDetailsFromFallIncidentDetails(validDetails))
                .build();

        entity =
                new HttpEntity<>(mapper.writeValueAsString(validForm), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    /**
     * Tests that invalid fall incidents are not created.
     * Invalid records are:
     * 1. With `value` >0 and empty `details`;
     * 2. With `value` >0 and invalid `details`. Details, containing strings that are not part of the list of accepted
     * fall causes, are considered invalid;
     * 3. With `value` of 0 and provided fall `details`. If no fall has occurred, providing fall details is not needed.
     */
    @Test
    void testRecordInvalidFallIncidentDetails() throws JsonProcessingException {
        // given: A fall incident form with missing details.
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);

        String invalidCause = "Test";
        String validCause1 = "Bad eyesight";
        String validCause2 = "Impaired balance";

        // Any value >0 indicates that there has been a fall with varying severity. When there is a fall,
        // (valid) details for the incident are required.
        double valueThatRequiresDetails = 1;
        double valueThatDoesNotRequireDetails = 0;

        UserMeasurementForm invalidForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .details(createDetailsFromFallIncidentDetails(FallIncidentDetails.builder().build()))
                .value(valueThatRequiresDetails)
                .build();

        // when: We try recording a fall incident with empty details
        HttpEntity<String> entity =
                new HttpEntity<>(mapper.writeValueAsString(invalidForm), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then: An error indicating that empty details are not accepted is thrown
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("details.empty"));

        // then when: We try recording a fall incident with one valid and one invalid fall cause
        FallIncidentDetails invalidDetails = FallIncidentDetails.builder()
                .causes(List.of(invalidCause, validCause1))
                .build();

        invalidForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(valueThatRequiresDetails)
                .details(createDetailsFromFallIncidentDetails(invalidDetails))
                .build();

        entity =
                new HttpEntity<>(mapper.writeValueAsString(invalidForm), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then: An error indicating that invalid details are not accepted is thrown
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("causes.invalid"));

        // then when: We try recording a no-fall incident with valid fall causes
        FallIncidentDetails validDetails = FallIncidentDetails.builder()
                .causes(List.of(validCause1, validCause2))
                .build();

        invalidForm = UserMeasurementForm.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .userId(user.getId())
                .recordedAt(LocalDateTime.now())
                .value(valueThatDoesNotRequireDetails)
                .details(createDetailsFromFallIncidentDetails(validDetails))
                .build();

        entity =
                new HttpEntity<>(mapper.writeValueAsString(invalidForm), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then: An error indicating that any details are forbidden is thrown
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("details.forbidden"));
    }

    /**
     * TODO.
     */
    @Test
    void testGetUserMeasurement() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);

        UserMeasurement measurement = UserMeasurement.builder()
                .type(UserMeasurementType.POMA)
                .user(user)
                .recordedAt(LocalDateTime.now())
                .recordedBy(authenticatedUser)
                .createdAt(LocalDateTime.now())
                .value(28.0)
                .build();
        measurement = userMeasurementRepository.save(measurement);

        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements" + '/' + measurement.getId(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        UserMeasurement measurementFromResponse = mapper.readValue(response.getBody(), UserMeasurement.class);

        // then
        assertEquals(authenticatedUser.getId(), measurementFromResponse.getRecordedBy().getId());
        assertEquals(user.getId(), measurementFromResponse.getUser().getId());
        assertEquals(
                measurement.getRecordedAt().truncatedTo(ChronoUnit.MILLIS),
                measurementFromResponse.getRecordedAt()
        );
        assertEquals(measurement.getType(), measurementFromResponse.getType());
        assertEquals(measurement.getValue(), measurementFromResponse.getValue());
    }

    /**
     * Tests getting all manual measurements for a user. In the case of Fall incidents, measurements with a `value` of 0
     * are not part of the response. Such (no-fall) measurements are stored in the DB and used for internal analysis.
     */
    @Test
    void testGetUserMeasurements() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);

        UserMeasurement visibleMeasurement1 = UserMeasurement.builder()
                .type(UserMeasurementType.POMA)
                .user(user)
                .recordedAt(LocalDateTime.now())
                .recordedBy(authenticatedUser)
                .createdAt(LocalDateTime.now())
                .value(28.0)
                .build();
        UserMeasurement visibleMeasurement2 = UserMeasurement.builder()
                .type(UserMeasurementType.POMA)
                .user(user)
                .recordedAt(LocalDateTime.now())
                .recordedBy(authenticatedUser)
                .createdAt(LocalDateTime.now())
                .value((0.0))
                .build();
        UserMeasurement visibleMeasurement3 = UserMeasurement.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .user(user)
                .recordedAt(LocalDateTime.now())
                .recordedBy(authenticatedUser)
                .createdAt(LocalDateTime.now())
                .value(1.0)
                .build();
        // Fall incidents with a value of 0 are excluded from API responses
        UserMeasurement invisibleMeasurement = UserMeasurement.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .user(user)
                .recordedAt(LocalDateTime.now())
                .recordedBy(authenticatedUser)
                .createdAt(LocalDateTime.now())
                .value(0.0)
                .build();

        visibleMeasurement1 = userMeasurementRepository.save(visibleMeasurement1);
        userMeasurementRepository.saveAll(List.of(
                visibleMeasurement2,
                visibleMeasurement3
        ));
        invisibleMeasurement = userMeasurementRepository.save(invisibleMeasurement);

        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + '/' + user.getId() + "/measurements", getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<UserMeasurement> measurements = mapper.readValue(
                response.getBody(),
                new TypeReference<List<UserMeasurement>>() {
                }
        );

        // then
        assertEquals(3, measurements.size());
        UserMeasurement finalInvisibleMeasurement = invisibleMeasurement;
        measurements.forEach(m -> assertNotEquals(m.getId(), finalInvisibleMeasurement.getId()));
        UserMeasurement firstMeasurementFromResponse = measurements.get(0);

        assertEquals(authenticatedUser.getId(), firstMeasurementFromResponse.getRecordedBy().getId());
        assertEquals(user.getId(), firstMeasurementFromResponse.getUser().getId());
        assertEquals(
                visibleMeasurement1.getRecordedAt().truncatedTo(ChronoUnit.MILLIS),
                firstMeasurementFromResponse.getRecordedAt()
        );
        assertEquals(visibleMeasurement1.getType(), firstMeasurementFromResponse.getType());
        assertEquals(visibleMeasurement1.getValue(), firstMeasurementFromResponse.getValue());
    }

    /**
     * TODO.
     */
    @Test
    void testGetUserMeasurementsByTypeWithinTimeFrame() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);
        final LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        UserMeasurement pomaMeasurementInTimeFrame = UserMeasurement.builder()
                .type(UserMeasurementType.POMA)
                .user(user)
                .recordedAt(currentTime)
                .recordedBy(authenticatedUser)
                .createdAt(currentTime)
                .value(28.0)
                .build();
        pomaMeasurementInTimeFrame = userMeasurementRepository.save(pomaMeasurementInTimeFrame);

        UserMeasurement pomaMeasurementOutOfTimeFrame = UserMeasurement.builder()
                .type(UserMeasurementType.POMA)
                .user(user)
                .recordedAt(currentTime.minusYears(1))
                .recordedBy(authenticatedUser)
                .createdAt(currentTime.minusYears(1))
                .value(28.0)
                .build();
        userMeasurementRepository.save(pomaMeasurementOutOfTimeFrame);

        UserMeasurement fallIncidentInTimeFrame = UserMeasurement.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .user(user)
                .recordedAt(currentTime)
                .recordedBy(authenticatedUser)
                .createdAt(currentTime)
                .value(28.0)
                .build();
        userMeasurementRepository.save(fallIncidentInTimeFrame);


        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USERS_REST_ENDPOINT + '/' + user.getId() +
                                "/measurements/poma" +
                                "?begin=" + currentTime.minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli() +
                                "&end=" + currentTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<UserMeasurement> measurements = mapper.readValue(
                response.getBody(),
                new TypeReference<List<UserMeasurement>>() {
                }
        );

        // then
        assertEquals(1, measurements.size());
        UserMeasurement measurementFromResponse = measurements.get(0);

        assertEquals(user.getId(), measurementFromResponse.getUser().getId());
        assertEquals(
                pomaMeasurementInTimeFrame.getRecordedAt().truncatedTo(ChronoUnit.MILLIS),
                measurementFromResponse.getRecordedAt()
        );
        assertEquals(pomaMeasurementInTimeFrame.getType(), measurementFromResponse.getType());
    }

    /**
     * TODO.
     */
    @Test
    void testSoftDeleteUserMeasurement() {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        userRepository.save(user);
        UserMeasurement userMeasurement = UserMeasurement.builder()
                .type(UserMeasurementType.POMA)
                .recordedAt(LocalDateTime.now())
                .value(28.0)
                .user(user)
                .recordedBy(authenticatedUser)
                .build();
        userMeasurement = userMeasurementRepository.save(userMeasurement);

        // when
        HttpEntity<String> entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USERS_REST_ENDPOINT + "/measurements/" + userMeasurement.getId(),
                        getPort()
                ),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        // then
        List<UserMeasurement> allMeasurements = userMeasurementRepository.findByUserId(user.getId());
        List<UserMeasurement> nonDeletedMeasurements =
                userMeasurementRepository.findByUserIdAndDeleted(user.getId(), false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, allMeasurements.size());
        assertEquals(0, nonDeletedMeasurements.size());
    }

    /**
     * TODO.
     */
    @Test
    void testGetMeasurementsAfterSoftDelete() throws JsonProcessingException {
        // given
        User user = User.builder().tenant(getTestTenant()).build();
        user = userRepository.save(user);

        UserMeasurement userMeasurement = UserMeasurement.builder()
                .type(UserMeasurementType.FALL_INCIDENT)
                .recordedAt(LocalDateTime.now())
                .value(21.0)
                .user(user)
                .createdAt(LocalDateTime.now())
                .recordedBy(authenticatedUser)
                .build();
        userMeasurement = userMeasurementRepository.save(userMeasurement);

        // when
        HttpEntity<String> entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + "/" + user.getId() + "/measurements", getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<UserMeasurement> userMeasurements =
                mapper.readValue(response.getBody(), new TypeReference<List<UserMeasurement>>() {
                });

        // then
        assertEquals(1, userMeasurements.size());

        // then given
        userMeasurement.setDeleted(true);
        userMeasurementRepository.save(userMeasurement);

        // then when
        entity = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT + "/" + user.getId() + "/measurements", getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        userMeasurements = mapper.readValue(response.getBody(), new TypeReference<List<UserMeasurement>>() {
        });

        // then
        assertEquals(0, userMeasurements.size());
    }

    /**
     * based on access by user.
     */
    @Test
    void testGetAccessibleApplications() throws JsonProcessingException {
        // given
        applicationRepository.deleteAll();

        Application first = Application.builder().name("Test").priority(1).users(List.of(authenticatedUser)).build();
        first = applicationRepository.save(first);

        Application second = Application.builder().name("Test_2").priority(2).users(List.of(authenticatedUser)).build();
        second = applicationRepository.save(second);

        Application third = Application.builder().name("Test_3").priority(3).build();
        applicationRepository.save(third);

        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_APPLICATIONS_REST_ENDPOINT, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<Application> accessible = mapper.readValue(response.getBody(), new TypeReference<List<Application>>() {
        });

        // then
        assertEquals(2, accessible.size());
        assertTrue(accessible.contains(first));
        assertTrue(accessible.contains(second));
    }

    /**
     * based on access by composite user.
     */
    @Test
    void testGetAccessibleApplicationsForCU() throws JsonProcessingException {
        // given
        CompositeUser compositeUser = CompositeUser.builder()
                .authId("testGetAccessibleAppsCU")
                .build();
        compositeUser = compositeUserRepository.save(compositeUser);
        User firstSubUser = User.builder()
                .tenant(getTestTenant())
                .authId("testGetAccessibleApps1")
                .compositeUser(compositeUser)
                .build();
        User secondSubUser = User.builder()
                .tenant(getTestTenant())
                .authId("testGetAccessibleApps2")
                .compositeUser(compositeUser)
                .build();
        User randomUser = User.builder()
                .tenant(getTestTenant())
                .authId("testGetAccessibleApps3")
                .build();
        firstSubUser = userRepository.save(firstSubUser);
        secondSubUser = userRepository.save(secondSubUser);
        randomUser = userRepository.save(randomUser);

        applicationRepository.deleteAll();
        Application first = Application.builder()
                .name("Test")
                .priority(1)
                .users(List.of(firstSubUser))
                .build();
        first = applicationRepository.save(first);

        Application second = Application.builder()
                .name("Test_2")
                .priority(2)
                .users(List.of(firstSubUser, secondSubUser))
                .build();
        second = applicationRepository.save(second);

        Application third = Application.builder()
                .name("Test_3")
                .priority(3)
                .users(List.of(randomUser))
                .build();
        applicationRepository.save(third);

        // when
        HttpHeaders cuHeaders = setCUWithUserAndGetHTTPHeaders(null, compositeUser.getAuthId());
        HttpEntity<String> entity = new HttpEntity<>(null, cuHeaders);
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_APPLICATIONS_REST_ENDPOINT, getPort()),
                HttpMethod.GET, entity, String.class
        );
        List<Application> accessible = mapper.readValue(response.getBody(), new TypeReference<List<Application>>() {
        });

        // then
        assertEquals(2, accessible.size());
        assertTrue(accessible.contains(first));
        assertTrue(accessible.contains(second));
    }

    /**
     * based on access by tenant.
     */
    @Test
    void testGetAccessibleApplicationsByTenant() throws JsonProcessingException {
        // given
        applicationRepository.deleteAll();

        List<User> users = new ArrayList<>();
        Application first = Application.builder()
                .name("Test")
                .priority(1)
                .users(List.of(authenticatedUser))
                .tenants(List.of(authenticatedUser.getTenant()))
                .build();
        first = applicationRepository.save(first);

        Application second = Application.builder()
                .name("Test_2")
                .priority(2)
                .users(List.of(authenticatedUser))
                .tenants(List.of(authenticatedUser.getTenant()))
                .build();
        applicationRepository.save(second);

        Application third = Application.builder()
                .name("Test_3")
                .priority(3)
                .users(List.of(authenticatedUser))
                .build();
        applicationRepository.save(third);

        Application fourth = Application.builder()
                .name("Test_4")
                .priority(4)
                .tenants(List.of(authenticatedUser.getTenant()))
                .build();
        applicationRepository.save(fourth);

        Application fifth = Application.builder().name("Test_5").priority(5).build();
        applicationRepository.save(fifth);

        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_APPLICATIONS_REST_ENDPOINT, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<Application> accessible = mapper.readValue(response.getBody(), new TypeReference<List<Application>>() {
        });
        // then
        assertEquals(4, accessible.size());
        assertTrue(accessible.contains(first));
        assertTrue(accessible.contains(second));
        assertTrue(accessible.contains(third));
        assertTrue(accessible.contains(fourth));
    }

    /**
     * TODO.
     */
    @Test
    void testGetUsersForTenant() throws JsonProcessingException {
        // given
        long secondTenantId = 2L;
        AccessScopeContext.INSTANCE.setTenantId(secondTenantId); // so that we are allowed to create for tenant 2
        Tenant tenant =
                tenantRepository.findById(secondTenantId).get(); // assumption is that it was created by migration
        User user = User.builder().tenant(tenant).build();
        userRepository.save(user);
        assertFalse(userRepository.findAll().isEmpty());
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());
        assertEquals(1, userRepository.findAll().size()); // only authenticated user for default tenant
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()), HttpMethod.GET, entity, String.class);
        List<User> users = mapper.readValue(response.getBody(), new TypeReference<List<User>>() {
        });
        // then: only the authenticated user is part of the users in the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, users.size());
        assertEquals(TestUtils.TEST_USER_AUTH_ID, users.get(0).getAuthId());
    }

    /**
     * TODO.
     */
    @Test
    void testCreateUserWhileUserLimitIsReached() throws JsonProcessingException {
        // given a group for a tenant which has a user limit of 1
        AccessScopeContext.INSTANCE.setTenantId(TENANT_WITH_USER_LIMIT_TENANT_ID);
        Group group = Group.builder()
                .tenant(getTestTenantWithUserLimit())
                .managers(List.of(authenticatedUserWithLimit))
                .name("test_group")
                .thingGroupName("test_thing_group")
                .build();
        groupRepository.save(group);

        // when we try to create two users for the group
        CreateWearableUserForm firstUserForm = CreateWearableUserForm.builder()
                .userGroupId(group.getId())
                .build();

        // use limited tenant's authenticated user (which manages the group) to (attempt to) create the users
        HttpHeaders headers = TestUtils.withAuthenticatedUserHttpHeaders(authenticatedUserWithLimit);

        HttpEntity<String> entity = new HttpEntity<>(
                mapper.writeValueAsString(firstUserForm),
                headers
        );
        ResponseEntity<String> responseFirstUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        CreateWearableUserForm secondUserForm = CreateWearableUserForm.builder()
                .userGroupId(group.getId())
                .build();
        HttpEntity<String> entity1 = new HttpEntity<>(
                mapper.writeValueAsString(secondUserForm),
                headers
        );
        ResponseEntity<String> responseSecondUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity1,
                String.class
        );

        // then
        assertEquals(HttpStatus.CREATED, responseFirstUserCreation.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, responseSecondUserCreation.getStatusCode());

        // then when
        HttpEntity<String> entity2 = new HttpEntity<>(headers);
        ResponseEntity<String> responseGetGroups = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT + "/" + group.getId(), getPort()),
                HttpMethod.GET,
                entity2,
                String.class
        );
        group = mapper.readValue(responseGetGroups.getBody(), new TypeReference<>() {
        });

        // then
        assertEquals(1, group.getUsers().size());
    }

    /**
     * TODO.
     */
    @Test
    void testCreateUserForTenantWithoutLimit() throws JsonProcessingException {
        // given
        Group group = Group.builder()
                .tenant(getTestTenant())
                .managers(List.of(authenticatedUser))
                .name("test_group")
                .thingGroupName("test_thing_group")
                .build();
        groupRepository.save(group);

        // when
        CreateWearableUserForm form = CreateWearableUserForm.builder()
                .userGroupId(group.getId())
                .build();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> responseFirstUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        CreateWearableUserForm form1 = CreateWearableUserForm.builder()
                .userGroupId(group.getId())
                .build();
        HttpEntity<String> entity1 = new HttpEntity<>(
                mapper.writeValueAsString(form1),
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> responseSecondUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity1,
                String.class
        );

        // then
        assertEquals(HttpStatus.CREATED, responseFirstUserCreation.getStatusCode());
        assertEquals(HttpStatus.CREATED, responseSecondUserCreation.getStatusCode());

        // then when
        HttpEntity<String> entity2 = new HttpEntity<>(TestUtils.defaultHttpHeaders());
        ResponseEntity<String> responseGetGroups = getRestTemplate().exchange(
                TestUtils.createURLWithPort(GROUPS_REST_ENDPOINT + "/" + group.getId(), getPort()),
                HttpMethod.GET,
                entity2,
                String.class
        );
        group = mapper.readValue(responseGetGroups.getBody(), new TypeReference<>() {
        });

        // then
        assertEquals(2, group.getUsers().size());
    }

    /**
     * TODO.
     */
    @Test
    void testCreateUserForTenantWithoutLimitAndGroupWithLimit() throws JsonProcessingException {
        //given
        Group group1 = Group.builder()
                .tenant(getTestTenant())
                .managers(List.of(authenticatedUser))
                .name("test_group_no_limit")
                .thingGroupName("test_thing_group")
                .build();
        groupRepository.save(group1);
        Group group2 = Group.builder()
                .tenant(getTestTenant())
                .managers(List.of(authenticatedUser))
                .name("test_group_with_limit")
                .thingGroupName("test_thing_group")
                .userLimit(DEFAULT_USER_LIMIT)
                .build();
        groupRepository.save(group2);
        //when
        CreateWearableUserForm form1 = CreateWearableUserForm.builder()
                .userGroupId(group1.getId())
                .build();
        HttpEntity<String> entity1 = new HttpEntity<>(mapper.writeValueAsString(form1), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> responseFirstUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity1,
                String.class
        );
        CreateWearableUserForm form2 = CreateWearableUserForm.builder()
                .userGroupId(group1.getId())
                .build();
        HttpEntity<String> entity2 = new HttpEntity<>(
                mapper.writeValueAsString(form2),
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> responseSecondUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity2,
                String.class
        );
        //then
        assertEquals(HttpStatus.CREATED, responseFirstUserCreation.getStatusCode());
        assertEquals(HttpStatus.CREATED, responseSecondUserCreation.getStatusCode());
        //then when
        CreateWearableUserForm form3 = CreateWearableUserForm.builder()
                .userGroupId(group2.getId())
                .build();
        HttpEntity<String> entity3 = new HttpEntity<>(mapper.writeValueAsString(form3), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> responseThirdUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity3,
                String.class
        );
        CreateWearableUserForm form4 = CreateWearableUserForm.builder()
                .userGroupId(group2.getId())
                .build();
        HttpEntity<String> entity4 = new HttpEntity<>(
                mapper.writeValueAsString(form4),
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> responseFourthUserCreation = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity4,
                String.class
        );
        // then
        assertEquals(HttpStatus.CREATED, responseThirdUserCreation.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, responseFourthUserCreation.getStatusCode());
    }

    /**
     * TODO.
     */
    @Test
    void testCreateUserWithInfo() throws JsonProcessingException {
        // given
        Group group = Group.builder()
                .tenant(getTestTenant())
                .managers(List.of(authenticatedUser))
                .name("test_group_create_user_with_info")
                .thingGroupName("test_thing_group")
                .build();
        groupRepository.save(group);
        UserInfo info = UserInfo.builder()
                .age(80)
                .shoes("test shoes")
                .notes("Creating user with info")
                .build();

        // when
        CreateWearableUserForm form = CreateWearableUserForm.builder()
                .userGroupId(group.getId())
                .info(info)
                .build();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USERS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        User user = mapper.readValue(response.getBody(), new TypeReference<>() {
        });

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(info.getAge(), user.getInfo().getAge());
        assertEquals(info.getShoes(), user.getInfo().getShoes());
        assertEquals(info.getNotes(), user.getInfo().getNotes());
    }

    /**
     * Serialize POMA measurement details to map for use as a details field in requests with UserMeasurementForm
     * objects.
     *
     * @return map of field names to values
     */
    private Map<String, Object> createDetailsFromPomaMeasurementDetails(PomaMeasurementDetails details) {
        Map<String, Object> map = new HashMap<>();
        map.put("sittingBalance", details.getSittingBalance());
        map.put("arises", details.getArises());
        map.put("attemptsToArise", details.getAttemptsToArise());
        map.put("immediateStandingBalance", details.getImmediateStandingBalance());
        map.put("standingBalance", details.getStandingBalance());
        map.put("nudged", details.getNudged());
        map.put("eyesClosed", details.getEyesClosed());
        map.put("turning360DegreesSteps", details.getTurning360DegreesSteps());
        map.put("turning360DegreesSteadiness", details.getTurning360DegreesSteadiness());
        map.put("sittingDown", details.getSittingDown());
        map.put("balanceTotal", details.getBalanceTotal());
        map.put("initiationOfGait", details.getInitiationOfGait());
        map.put("stepLengthHeightRightPassesLeft", details.getStepLengthHeightRightPassesLeft());
        map.put("stepLengthHeightRightClearsFloor", details.getStepLengthHeightRightClearsFloor());
        map.put("stepLengthHeightLeftPassesRight", details.getStepLengthHeightLeftPassesRight());
        map.put("stepLengthHeightLeftClearsFloor", details.getStepLengthHeightLeftClearsFloor());
        map.put("stepSymmetry", details.getStepSymmetry());
        map.put("stepContinuity", details.getStepContinuity());
        map.put("path", details.getPath());
        map.put("trunk", details.getTrunk());
        map.put("walkingStance", details.getWalkingStance());
        map.put("mobilityTotal", details.getMobilityTotal());
        return map;
    }

    /**
     * Formats fall incident details to a map, containing a field "causes". In case of a "no-fall" record,
     * the details are empty (which is the default DB value of that field).
     *
     * @param details FallIncidentDetails
     * @return Map of fall incident details
     */
    private Map<String, Object> createDetailsFromFallIncidentDetails(FallIncidentDetails details) {
        Map<String, Object> map = new HashMap<>();
        List causes = details.getCauses();
        if (causes != null) {
            map.put("causes", causes);
        }
        return map;
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
                .withCompositeUserId(compositeUser.getId())
                .withUserType(UserType.COMPOSITE_USER)
                .withAuthId(compositeUser.getAuthId())
                .withRole(Role.USER)
                .build();
    }
}
