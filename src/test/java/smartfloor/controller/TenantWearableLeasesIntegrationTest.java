package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import smartfloor.domain.dto.UnlinkActiveWearableForm;
import smartfloor.domain.dto.UserWearableLinkForm;
import smartfloor.domain.dto.rehabilitation.TestResultForm;
import smartfloor.domain.dto.rehabilitation.WearableForm;
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
import smartfloor.domain.entities.fall.risk.profile.V1FallRiskScoreAssessment;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestTrial;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.domain.entities.rehabilitation.WearableWithSide;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.tests.TenMeterWalking;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.FallRiskProfileRepository;
import smartfloor.repository.jpa.FloorRepository;
import smartfloor.repository.jpa.FootstepRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.TestResultRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.UserWearableLinkRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

/**
 * <p>This test suite evaluates the tenant access to resources related to wearables (footsteps, fall risk profiles,
 * rehabilitation test results, user wearable links). This is done by mainly using two
 * test tenants (with IDs 2 and 3), and each of them have a tenant lease with a single wearable - heelable_51. Those
 * leases are created by a test migration (V48.1). The first lease is terminated, while the second one is ongoing.</p>
 * Essentially, tests here make sure that:
 * <p>1. An active/ongoing lease allows you to create, modify and view resources related to the test wearable;</p>
 * <p>2. A terminated lease only allows you to view resources related to the test wearable, but you aren't allowed to
 * create new resources or modify existing ones;</p>
 * <p>3. Never having a lease with the test wearable forbids all access to wearable related resources. (The wearable
 * will not even be visible for such a tenant.)</p>
 */
class TenantWearableLeasesIntegrationTest extends IntegrationTestBase {
    private static final String ENDPOINT_FOOTSTEPS_WEARABLES = "/footsteps/wearables/";
    private static final String ENDPOINT_REHABILITATION_RESULTS = "/rehabilitation/tests/results";
    private static final String ENDPOINT_COMPUTED_10MWT_WEARABLES =
            "/v1/analyses/rehabilitation/tests/ten-meter-walking/wearables/";
    private static final String ENDPOINT_COMPUTED_10MWT_USERS =
            "/v1/analyses/rehabilitation/tests/ten-meter-walking/users/";
    private static final String ENDPOINT_UWL = "/user-wearable-links";
    private static final String FIRST_GROUP_MANAGER = "smartfloor-test-gm-1";
    private static final String SECOND_GROUP_MANAGER = "smartfloor-test-gm-2";
    private static final String FIRST_REGULAR_USER = "u-1";
    private static final String SECOND_REGULAR_USER = "u-2";
    // Leased by tenants with IDs 2 and 3. Never leased by tenant with ID 1 (the default test tenant in most integration
    // tests).
    private static final String TEST_WEARABLE_ID = "heelable_51";
    // Those timestamps are used in the migration for creating the leases, do NOT change them.
    private static final String FIRST_LEASE_BEGIN = "1693555200000";
    private static final LocalDateTime FIRST_LEASE_BEGIN_LDT =
            LocalDateTime.of(2023, 9, 2, 10, 0, 0);
    private static final String FIRST_LEASE_END = "1693814400000";
    private static final LocalDateTime FIRST_LEASE_END_LDT =
            FIRST_LEASE_BEGIN_LDT.plusSeconds(2);
    private static final String SECOND_LEASE_BEGIN = "1693814460000";
    private static final LocalDateTime SECOND_LEASE_BEGIN_LTD = LocalDateTime.of(2023, 9, 5, 10, 0, 0);
    // The second lease has no end time, so it is still active.
    private static final long currentTimeInMs = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private WearableRepository wearableRepository;
    @Autowired
    private WearableGroupRepository wearableGroupRepository;
    @Autowired
    private FootstepRepository footstepRepository;
    @Autowired
    private FloorRepository floorRepository;
    @Autowired
    private FallRiskProfileRepository fallRiskProfileRepository;
    @Autowired
    private TestResultRepository testResultRepository;
    @Autowired
    private UserWearableLinkRepository userWearableLinkRepository;
    @Autowired
    CompositeUserRepository compositeUserRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * TODO.
     */
    Tenant getTestTenant(Long tenantId) {
        return tenantRepository.findById(tenantId).get();
    }

    /**
     * TODO.
     */
    User getUser(String authId, Tenant tenant) {
        return userRepository.findByAuthId(authId).orElseGet(() -> userRepository.save(User.builder()
                .authId(authId)
                .tenant(tenant)
                .build())
        );
    }

    /**
     * TODO.
     */
    Group getGroup(
            String name,
            Tenant tenant,
            List<User> managers,
            List<User> users,
            WearableGroup wearableGroup
    ) {
        return groupRepository.findByName(name).orElseGet(() -> groupRepository.save(Group.builder()
                .name(name)
                .tenant(tenant)
                .managers(managers)
                .users(users)
                .wearableGroup(wearableGroup)
                .build())
        );
    }

    /**
     * TODO.
     */
    Floor getFloor(String name, List<User> viewers) {
        return floorRepository.findByName(name).orElseGet(() -> floorRepository.save(Floor.builder()
                .name(name)
                .orientationNorth(0.00)
                .rotation(0.00)
                .maxX(3000)
                .maxY(10000)
                .viewers(viewers)
                .build())
        );
    }

    /**
     * TODO.
     */
    TestResult getTestResult(User user, Wearable wearable, LocalDateTime begin, LocalDateTime end) {
        List<TestResult> trs = testResultRepository.findByUserIdIn(List.of(user.getId()));
        if (trs.size() == 0) {
            TestTrial t = TestTrial.builder()
                    .beginTime(begin)
                    .endTime(end)
                    .build();
            return testResultRepository.save(TestResult.builder()
                    .wearableWithSide(new WearableWithSide(wearable, Wearable.Side.RIGHT))
                    .user(user)
                    .beginTime(begin)
                    .endTime(end)
                    .type(TestType.TEN_METER_WALKING)
                    .trials(List.of(t))
                    .build());
        }
        return trs.get(0);
    }

    /**
     * TODO.
     */
    FallRiskProfile getFRP(Floor floor, Wearable wearable, LocalDateTime begin, LocalDateTime end) {
        List<FallRiskProfile> frps =
                fallRiskProfileRepository.findByWearableIdAndCreationTimeBetweenOrderByCreationTime(
                        TEST_WEARABLE_ID, begin, end
                );
        if (frps.size() == 0) {
            return fallRiskProfileRepository.save(FallRiskProfile.builder()
                    .wearable(wearable)
                    .floor(floor)
                    .beginTime(begin)
                    .endTime(end)
                    .walkingSpeed(500.0)
                    .stepLength(300.0)
                    .stepFrequency(300.0)
                    .creationTime(end)
                    .build()
            );
        }
        return frps.get(0);
    }

    /**
     * TODO.
     */
    UserWearableLink getUWL(User user, Wearable wearable, LocalDateTime begin, LocalDateTime end) {
        List<UserWearableLink> uwls =
                userWearableLinkRepository.findByUserIdOrderByBeginTimeAsc(user.getId(), UserWearableLink.class);
        if (uwls.size() == 0) {
            return userWearableLinkRepository.save(UserWearableLink.builder()
                    .wearable(wearable)
                    .user(user)
                    .side(Wearable.Side.RIGHT)
                    .beginTime(begin)
                    .endTime(end)
                    .build()
            );
        }
        return uwls.get(0);
    }

    /**
     * TODO.
     */
    WearableGroup getWearableGroup(Wearable wearable, String groupName) {
        return wearableGroupRepository.findByName(groupName)
                .orElseGet(() -> wearableGroupRepository.save(WearableGroup.builder()
                        .name(groupName)
                        .wearables(List.of(wearable))
                        .build())
                );
    }

    /**
     * TODO.
     */
    @BeforeEach
    void setup() {
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(2L);
        testResultRepository.deleteAll();
        Tenant tenant1 = getTestTenant(2L);
        User manager1 = getUser(FIRST_GROUP_MANAGER, tenant1);
        User user1 = getUser(FIRST_REGULAR_USER, tenant1);
        Group group1 = getGroup("leases-group-1", tenant1, List.of(manager1), List.of(user1), null);
        Floor floor1 = getFloor("floor-1", List.of(manager1));
        Wearable testWearable = wearableRepository.findById(TEST_WEARABLE_ID);
        // Create 3 footsteps for the first tenant.
        Footstep footstep1 = Footstep.builder()
                .wearable(testWearable)
                .time(FIRST_LEASE_BEGIN_LDT)
                .floor(floor1)
                .position(new Position(1200, 1000))
                .build();
        Footstep footstep2 = Footstep.builder()
                .wearable(testWearable)
                .time(FIRST_LEASE_BEGIN_LDT.plusSeconds(1))
                .floor(floor1)
                .position(new Position(1200, 1500))
                .build();
        Footstep footstep3 = Footstep.builder()
                .wearable(testWearable)
                .time(FIRST_LEASE_END_LDT)
                .floor(floor1)
                .position(new Position(1200, 2000))
                .build();
        footstepRepository.saveAll(List.of(footstep1, footstep2, footstep3));
        getTestResult(
                user1, testWearable,
                FIRST_LEASE_BEGIN_LDT,
                FIRST_LEASE_END_LDT
        );
        getFRP(
                floor1, testWearable,
                FIRST_LEASE_BEGIN_LDT,
                FIRST_LEASE_END_LDT
        );
        getUWL(
                user1, testWearable,
                FIRST_LEASE_BEGIN_LDT,
                FIRST_LEASE_END_LDT
        );

        AccessScopeContext.INSTANCE.setTenantId(3L);
        testResultRepository.deleteAll();
        Tenant t2 = getTestTenant(3L);
        User gm2 = getUser(SECOND_GROUP_MANAGER, t2);
        User u2 = getUser(SECOND_REGULAR_USER, t2);
        Group g2 = getGroup(
                "leases-group-2",
                t2,
                List.of(gm2),
                List.of(u2),
                null
        );
        Floor f2 = getFloor("floor-2", List.of(gm2));
        // Create only 2 footsteps for the second tenant.
        Footstep fs4 = Footstep.builder()
                .wearable(testWearable)
                .time(SECOND_LEASE_BEGIN_LTD)
                .floor(f2)
                .position(new Position(1200, 1000))
                .build();
        Footstep fs5 = Footstep.builder()
                .wearable(testWearable)
                .time(SECOND_LEASE_BEGIN_LTD.plusSeconds(1))
                .floor(f2)
                .position(new Position(1200, 1900))
                .build();
        footstepRepository.saveAll(List.of(fs4, fs5));
        getTestResult(
                u2, testWearable,
                SECOND_LEASE_BEGIN_LTD,
                SECOND_LEASE_BEGIN_LTD.plusSeconds(1)
        );
        getFRP(
                f2, testWearable,
                SECOND_LEASE_BEGIN_LTD,
                SECOND_LEASE_BEGIN_LTD.plusSeconds(1)
        );
        getUWL(
                u2, testWearable,
                SECOND_LEASE_BEGIN_LTD,
                SECOND_LEASE_BEGIN_LTD.plusSeconds(1)
        );
    }

    /**
     * TODO.
     */
    @AfterEach
    void teardown() {
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(2L);
        footstepRepository.deleteAll();
        userWearableLinkRepository.deleteAll();
        fallRiskProfileRepository.deleteAll();
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(3L);
        footstepRepository.deleteAll();
        userWearableLinkRepository.deleteAll();
        fallRiskProfileRepository.deleteAll();
    }

    @Test
    void testGetWearableFootstepsForTenantWithTerminatedLease() throws JsonProcessingException {
        // given: We use a tenant, who has a (terminated) lease with the target wearable.
        AccessScopeContext.INSTANCE.setTenantId(2L);
        User gm1 = userRepository
                .findByAuthId(FIRST_GROUP_MANAGER)
                .orElseThrow(
                        () -> new RuntimeException("Test user not found")
                );

        // when: We try getting all footsteps for the test wearable, providing a time window which encompasses both
        // tenant-wearable leases
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(
                        AccessScopeContext.INSTANCE.getTenantId(),
                        gm1,
                        Role.MANAGER,
                        UserType.DIRECT_USER
                )
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_FOOTSTEPS_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN +
                                "&end=" + currentTimeInMs,
                        getPort()
                ), HttpMethod.GET, entity, String.class);
        List<Footstep> footsteps = mapper.readValue(response.getBody(), List.class);

        // then: We created 3 footsteps during the lease of the first tenant, so only those should be visible for them.
        assertEquals(3, footsteps.size());

        // Then we perform the same test, this time authenticating as a composite user (CU)
        User regularUser = userRepository.findByAuthId(FIRST_REGULAR_USER).get();
        HttpHeaders cuHeaders =
                setCUWithUserAndGetHTTPHeaders(regularUser, "testGetWearableForTenantWithTerminatedLease");

        entity = new HttpEntity<>(null, cuHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_FOOTSTEPS_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN +
                                "&end=" + currentTimeInMs,
                        getPort()
                ), HttpMethod.GET, entity, String.class);
        footsteps = mapper.readValue(response.getBody(), List.class);

        // then: We created 3 footsteps during the lease of the first tenant, so only those should be visible for them.
        assertEquals(3, footsteps.size());
    }

    @Test
    void testGetWearableFootstepsForTenantWithActiveLease() throws JsonProcessingException {
        // given: We use a tenant, who has a (active) lease with the target wearable.
        AccessScopeContext.INSTANCE.setTenantId(3L);
        User gm2 = userRepository
                .findByAuthId(SECOND_GROUP_MANAGER)
                .orElseThrow(
                        () -> new RuntimeException("Test user not found")
                );

        // when: We try getting all footsteps for the test wearable, providing a time window which encompasses both
        // tenant-wearable leases
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(
                        AccessScopeContext.INSTANCE.getTenantId(),
                        gm2,
                        Role.MANAGER,
                        UserType.DIRECT_USER
                )
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_FOOTSTEPS_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN +
                                "&end=" + currentTimeInMs,
                        getPort()
                ), HttpMethod.GET, entity, String.class);
        List<Footstep> footsteps = mapper.readValue(response.getBody(), List.class);

        // then: We created 2 footsteps during the lease of the second tenant, so only those should be visible for them.
        assertEquals(2, footsteps.size());

        // Then we perform the same test, this time authenticating as a CU
        User regularUser = userRepository.findByAuthId(SECOND_REGULAR_USER).get();
        HttpHeaders cuHeaders = setCUWithUserAndGetHTTPHeaders(
                regularUser, "testGetWearableFootstepsForTenantWithActiveLease"
        );

        entity = new HttpEntity<>(null, cuHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_FOOTSTEPS_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN +
                                "&end=" + currentTimeInMs,
                        getPort()
                ), HttpMethod.GET, entity, String.class);
        footsteps = mapper.readValue(response.getBody(), List.class);

        // then: We created 2 footsteps during the lease of the second tenant, so only those should be visible for them.
        assertEquals(2, footsteps.size());
    }

    @Test
    void testTenantNotAllowedToGetFootstepsForWearableTheyHaveNeverLeased() throws JsonProcessingException {
        // given: We use a tenant, who has never had a lease with the target wearable.
        AccessScopeContext.INSTANCE.setTenantId(1L);
        // when: We call the same endpoint that was called in the previous two tests.
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_FOOTSTEPS_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN +
                                "&end=" + currentTimeInMs,
                        getPort()
                ), HttpMethod.GET, entity, String.class);

        // then: Since the default tenant never had a lease with heelable_51, the RLS policy essentially hides it
        // for that tenant, resulting in a "Not found" error.
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Wearable with id heelable_51 does not exist.", response.getBody());

        // Then we perform the same test, this time authenticating as a CU
        User regularUser = User.builder()
                .tenant(Tenant.getDefaultTenant())
                .authId("testTenantNotAllowedToGetFootstepsForWearableTheyHaveNeverLeased")
                .build();
        regularUser = userRepository.save(regularUser);
        HttpHeaders cuHeaders = setCUWithUserAndGetHTTPHeaders(
                regularUser, "testTenantNotAllowedToGetFootstepsForWearableTheyHaveNeverLeased"
        );

        entity = new HttpEntity<>(null, cuHeaders);
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_FOOTSTEPS_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN +
                                "&end=" + currentTimeInMs,
                        getPort()
                ), HttpMethod.GET, entity, String.class);

        // then: Since the default tenant never had a lease with heelable_51, the RLS policy essentially hides it
        // for that tenant, resulting in a "Not found" error.
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Wearable with id heelable_51 does not exist.", response.getBody());
    }

    @Test
    void testGetRawTRsOnlyForCurrentTenant() throws JsonProcessingException {
        // given: Tenants with IDs 2L and 3L each have one TR (created in #setup).
        // We set the current tenant to 2L
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = tenantRepository.findById(2L).get();
        User manager = getUser(FIRST_GROUP_MANAGER, tenant);
        User user = getUser(FIRST_REGULAR_USER, tenant);
        // when: We get all TRs
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(ENDPOINT_REHABILITATION_RESULTS, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<TestResult> trs = mapper.readValue(response.getBody(), new TypeReference<List<TestResult>>() {
        });
        // then: We expect to see only one TR, and it should belong to the only user of this tenant
        assertEquals(1, trs.size());
        assertEquals(user.getId(), trs.get(0).getUser().getId());

        // then given: We change the tenant
        AccessScopeContext.INSTANCE.setTenantId(3L);
        tenant = tenantRepository.findById(3L).get();
        manager = getUser(SECOND_GROUP_MANAGER, tenant);
        user = getUser(SECOND_REGULAR_USER, tenant);
        // then when: We get all TRs
        entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(ENDPOINT_REHABILITATION_RESULTS, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        trs = mapper.readValue(response.getBody(), new TypeReference<List<TestResult>>() {
        });
        // then: We expect to see one TR, which belongs to the only user of this tenant
        assertEquals(1, trs.size());
        assertEquals(user.getId(), trs.get(0).getUser().getId());
    }

    @Test
    void testTenantWithTerminatedLeaseNotAllowedToCreateTRs() throws JsonProcessingException {
        // given: Set tenant to 2L. They have a terminated lease with the test wearable, so they should be able to see
        // the wearable, but they should NOT be allowed to use it for TR creation.
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = tenantRepository.findById(2L).get();
        User manager = getUser(FIRST_GROUP_MANAGER, tenant);
        User user = getUser(FIRST_REGULAR_USER, tenant);
        Wearable w = wearableRepository.findById(TEST_WEARABLE_ID);
        TestResultForm trForm = TestResultForm.builder()
                .wearable(new WearableForm(w.getId(), Wearable.Side.RIGHT))
                .userId(user.getId())
                .beginTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now())
                .type(TestType.TEN_METER_WALKING)
                .build();
        // when: We try creating a TR
        HttpEntity<String> entity = new HttpEntity<>(
                mapper.writeValueAsString(trForm),
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(ENDPOINT_REHABILITATION_RESULTS, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        // then: Should throw 403, Forbidden
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().contains("has no access to wearable heelable_51."));
    }

    @Test
    void testTenantWithNoPreviousLeaseNotAllowedToCreateTRs() throws JsonProcessingException {
        // given: Set tenant to 1L. They have NEVER had a lease with the test wearable, so they should NOT even be able
        // to look it up, let alone create a TR.
        AccessScopeContext.INSTANCE.setTenantId(1L);
        User user = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID)
                .orElseGet(() -> userRepository.save(TestUtils.testUser()));
        TestResultForm trForm = TestResultForm.builder()
                .wearable(new WearableForm(TEST_WEARABLE_ID, Wearable.Side.RIGHT))
                .userId(user.getId())
                .beginTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now())
                .type(TestType.TEN_METER_WALKING)
                .build();
        // when: We try creating a TR
        HttpEntity<String> entity = new HttpEntity<>(
                mapper.writeValueAsString(trForm),
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(ENDPOINT_REHABILITATION_RESULTS, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        // then: Should throw 404, Not found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Wearable with id heelable_51 does not exist.", response.getBody());
    }

    @Test
    void testTenantWithNoPreviousLeaseNotAllowedToComputeResultsFromFootsteps() throws JsonProcessingException {
        // given: We set tenant to the default one (no previous leases to the test wearable)
        AccessScopeContext.INSTANCE.setTenantId(1L);

        // when: We try computing a result from footsteps of the test wearable
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_COMPUTED_10MWT_WEARABLES + TEST_WEARABLE_ID + "?begin=" + FIRST_LEASE_BEGIN +
                                "&end=" + currentTimeInMs,
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        // then: Should throw 404, Not found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Wearable with id heelable_51 does not exist.", response.getBody());
    }

    @Test
    void testTenantWithTerminatedLeaseAllowedToComputeResultsFromFootsteps() throws JsonProcessingException {
        // given: Set tenant to the one with a terminated lease
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = getTestTenant(2L);
        User manager = getUser(FIRST_GROUP_MANAGER, tenant);
        AverageSpeed expectedAvgSpeed = AverageSpeed.of(footstepRepository.findAll());
        // when: We try computing a result from footsteps of the test wearable
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(2L, manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_COMPUTED_10MWT_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs,
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        TenMeterWalking tmwt = mapper.readValue(response.getBody(), new TypeReference<TenMeterWalking>() {
        });
        // then: Should get a computed result with non-null indicators (Only indicator of this test is speed)
        assertEquals(
                expectedAvgSpeed.getValue().doubleValue(),
                tmwt.getIndicatorByName("AverageSpeed").getValue().doubleValue(),
                0.0
        );
        assertTrue(expectedAvgSpeed.getValue() > 0);
    }

    @Test
    void testTenantWithActiveLeaseAllowedToComputeResultsFromFootsteps() throws JsonProcessingException {
        // given: Set tenant to the one with an active lease
        AccessScopeContext.INSTANCE.setTenantId(3L);
        Tenant tenant = getTestTenant(3L);
        User manager = getUser(SECOND_GROUP_MANAGER, tenant);
        AverageSpeed expectedAvgSpeed = AverageSpeed.of(footstepRepository.findAll());
        // when: We try computing a result from footsteps of the test wearable
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_COMPUTED_10MWT_WEARABLES + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs,
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        TenMeterWalking tmwt = mapper.readValue(response.getBody(), new TypeReference<TenMeterWalking>() {
        });
        // then: Should get a computed result with non-null indicators (Only indicator of this test is speed)
        assertEquals(
                expectedAvgSpeed.getValue().doubleValue(),
                tmwt.getIndicatorByName("AverageSpeed").getValue().doubleValue(),
                0.0
        );
        assertTrue(expectedAvgSpeed.getValue() > 0);
    }

    @Test
    void testComputingTestFromTRsForTenantWithNoAssociatedTRsReturnsEmptyList() throws JsonProcessingException {
        // given: We set tenant to the default one (no TestResults are created for them)
        AccessScopeContext.INSTANCE.setTenantId(1L);
        User user = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID)
                .orElseGet(() -> userRepository.save(TestUtils.testUser()));
        // when: We try computing a result from TestResults
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_COMPUTED_10MWT_USERS + user.getId() +
                                "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs,
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<TenMeterWalking> results =
                mapper.readValue(response.getBody(), new TypeReference<List<TenMeterWalking>>() {
                });
        // then: Expect an empty array of computed results
        assertEquals(0, results.size());
    }

    @Test
    void testComputeTestFromTRsForTenantWithTerminatedLease() throws JsonProcessingException {
        // given: Set tenant to the one with a terminated lease
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = getTestTenant(2L);
        User manager = getUser(FIRST_GROUP_MANAGER, tenant);
        User user = getUser(FIRST_REGULAR_USER, tenant);
        AverageSpeed expectedAvgSpeed = AverageSpeed.of(footstepRepository.findAll());
        // when: We try computing a result from existing TRs
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_COMPUTED_10MWT_USERS + user.getId() +
                                "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs,
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<TenMeterWalking> results =
                mapper.readValue(response.getBody(), new TypeReference<List<TenMeterWalking>>() {
                });
        // then: Should get an array of a single (because we create only one TR during #setup) computed result with
        // non-null indicators (Only indicator of this test is speed)
        assertEquals(
                expectedAvgSpeed.getValue().doubleValue(),
                results.get(0).getIndicatorByName("AverageSpeed").getValue().doubleValue(),
                0.0
        );
        assertTrue(expectedAvgSpeed.getValue() > 0);
    }

    @Test
    void testComputeTestFromTRsForTenantWithActiveLease() throws JsonProcessingException {
        // given: Set tenant to the one with an active lease
        AccessScopeContext.INSTANCE.setTenantId(3L);
        Tenant tenant = getTestTenant(3L);
        User manager = getUser(SECOND_GROUP_MANAGER, tenant);
        User user = getUser(SECOND_REGULAR_USER, tenant);
        AverageSpeed expectedAvgSpeed = AverageSpeed.of(footstepRepository.findAll());
        // when: We try computing a result from existing TRs
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        ENDPOINT_COMPUTED_10MWT_USERS + user.getId() +
                                "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs,
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<TenMeterWalking> results =
                mapper.readValue(response.getBody(), new TypeReference<List<TenMeterWalking>>() {
                });
        // then: Should get an array of a single (because we create only one TR during #setup) computed result with
        // non-null indicators (Only indicator of this test is speed)
        assertEquals(
                expectedAvgSpeed.getValue().doubleValue(),
                results.get(0).getIndicatorByName("AverageSpeed").getValue().doubleValue(),
                0.0
        );
        assertTrue(expectedAvgSpeed.getValue() > 0);
    }

    @Test
    void testTenantWithNoPreviousLeaseNotAllowedToCreateUWL() throws JsonProcessingException {
        // given: We set tenant to the default one (has never leased the test wearable)
        AccessScopeContext.INSTANCE.setTenantId(1L);
        User user = userRepository.findByAuthId(TestUtils.TEST_USER_AUTH_ID)
                .orElseGet(() -> userRepository.save(TestUtils.testUser()));
        UserWearableLinkForm uwlForm = new UserWearableLinkForm(
                user.getId(),
                TEST_WEARABLE_ID,
                LocalDateTime.now(),
                null,
                Wearable.Side.RIGHT
        );
        // when: We try creating a link with the test wearable
        HttpEntity<String> entity = new HttpEntity<>(
                mapper.writeValueAsString(uwlForm),
                TestUtils.defaultHttpHeaders()
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(ENDPOINT_UWL, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        // then: Wearable not found, 404
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Wearable with id heelable_51 does not exist.", response.getBody());
    }

    @Test
    void testTenantWithTerminatedLeaseAllowedToCreateUWL() throws JsonProcessingException {
        // given: We set tenant to the one with a terminated lease
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = getTestTenant(2L);
        User manager = getUser(FIRST_GROUP_MANAGER, tenant);
        User user = getUser(FIRST_REGULAR_USER, tenant);
        UserWearableLinkForm uwlForm = new UserWearableLinkForm(
                user.getId(),
                TEST_WEARABLE_ID,
                LocalDateTime.now(),
                null,
                Wearable.Side.RIGHT
        );
        // when: We try creating a link with the test wearable
        HttpEntity<String> entity = new HttpEntity<>(
                mapper.writeValueAsString(uwlForm),
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(ENDPOINT_UWL, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        // then: No access, 403
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().contains("has no access to wearable heelable_51."));
    }

    @Test
    void testTenantWithTerminatedLeaseNotAllowedToUnlinkWearable() throws JsonProcessingException {
        // given: We set tenant to the one with a terminated lease
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = getTestTenant(2L);
        User manager = getUser(FIRST_GROUP_MANAGER, tenant);
        User user = getUser(FIRST_REGULAR_USER, tenant);
        UnlinkActiveWearableForm unlinkForm = new UnlinkActiveWearableForm(TEST_WEARABLE_ID);

        // when: We try unlinking the test wearable
        HttpEntity<String> entity = new HttpEntity<>(
                mapper.writeValueAsString(unlinkForm),
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(ENDPOINT_UWL + "/wearables/unlink", getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        // then: Status 403, Forbidden
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().contains("has no access to wearable heelable_51."));
    }

    @Test
    void testTenantWithTerminatedLeaseAllowedToViewFRPs() throws JsonProcessingException {
        // given: We set tenant to the one with a terminated lease
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = getTestTenant(2L);
        User manager = getUser(FIRST_GROUP_MANAGER, tenant);
        User user = getUser(FIRST_REGULAR_USER, tenant);

        // when: We try getting a FallRiskAssessment for a wearable
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/wearables/" + TEST_WEARABLE_ID +
                        "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then: Status should be 200, OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // then when: We try getting all FallRiskAssessments for the test user
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() +
                        "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<FallRiskAssessmentModel> fallRiskAssessments = Arrays.asList(
                mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class)
        );

        // then: Status should be 200, OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, fallRiskAssessments.size());

        // then when: We try getting all V1FallRiskScoreAssessments for the test user
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/v1/analyses/fall-risk/users/" + user.getId() +
                        "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<V1FallRiskScoreAssessment> v1FallRiskAssessments =
                Arrays.asList(mapper.readValue(response.getBody(), V1FallRiskScoreAssessment[].class));

        // then: Status should be 200, OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, v1FallRiskAssessments.size());
    }

    @Test
    void testTenantWithActiveLeaseAllowedToViewFRPs() throws JsonProcessingException {
        // given: We set tenant to the one with an active lease
        AccessScopeContext.INSTANCE.setTenantId(3L);
        Tenant tenant = getTestTenant(3L);
        User manager = getUser(SECOND_GROUP_MANAGER, tenant);
        User user = getUser(SECOND_REGULAR_USER, tenant);

        // when: We try getting a FallRiskAssessment for a wearable
        HttpEntity<String> entity = new HttpEntity<>(
                null,
                TestUtils.httpHeadersBuilder(tenant.getId(), manager, Role.MANAGER, UserType.DIRECT_USER)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        "/analyses/fall-risk/wearables/" + TEST_WEARABLE_ID +
                                "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs,
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );

        // then: Status should be 200, OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // then when: We try getting all FallRiskAssessments for the test user
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/analyses/fall-risk/users/" + user.getId() +
                        "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<FallRiskAssessmentModel> fallRiskAssessments = Arrays.asList(
                mapper.readValue(response.getBody(), FallRiskScoreAssessment[].class)
        );

        // then: Status should be 200, OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, fallRiskAssessments.size());

        // then when: We try getting all V1FallRiskScoreAssessments for the test user
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/v1/analyses/fall-risk/users/" + user.getId() +
                        "?begin=" + FIRST_LEASE_BEGIN + "&end=" + currentTimeInMs, getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<V1FallRiskScoreAssessment> v1FallRiskAssessments = Arrays.asList(
                mapper.readValue(response.getBody(), V1FallRiskScoreAssessment[].class)
        );

        // then: Status should be 200, OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, v1FallRiskAssessments.size());
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
