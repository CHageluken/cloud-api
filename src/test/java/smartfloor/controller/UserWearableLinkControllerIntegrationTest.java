package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.dto.UnlinkActiveUserForm;
import smartfloor.domain.dto.UnlinkActiveWearableForm;
import smartfloor.domain.dto.UserWearableLinkForm;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.FootstepRepository;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.repository.jpa.UserWearableLinkRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

class UserWearableLinkControllerIntegrationTest extends IntegrationTestBase {

    private static final String USER_WEARABLE_LINKS_REST_ENDPOINT = "/user-wearable-links";
    @Autowired
    UserRepository userRepository;
    @Autowired
    FootstepRepository footstepRepository;
    @Autowired
    WearableRepository wearableRepository;
    @Autowired
    UserWearableLinkRepository userWearableLinkRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    WearableGroupRepository wearableGroupRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * TODO.
     */
    Wearable getTestWearable() {
        Wearable w = wearableRepository.findById("testWearableForUWLs");
        if (w == null) {
            w = Wearable.builder()
                    .id("testWearableForUWLs")
                    .build();
            w = wearableRepository.save(w);
        }
        return w;
    }

    /**
     * TODO.
     */
    Wearable getOtherTestWearable() {
        Wearable w = wearableRepository.findById("testOtherWearableForUWLs");
        if (w == null) {
            w = Wearable.builder()
                    .id("testOtherWearableForUWLs")
                    .build();
            w = wearableRepository.save(w);
        }
        return w;
    }

    /**
     * TODO.
     */
    WearableGroup getTestWearableGroup() {
        return wearableGroupRepository.findByName("testWearableGroupForUWLs")
                .orElseGet(() -> wearableGroupRepository.save(
                        WearableGroup.builder()
                                .name("testWearableGroupForUWLs")
                                .wearables(List.of(getTestWearable(), getOtherTestWearable()))
                                .build()
                ));
    }

    /**
     * TODO.
     */
    Group getTestGroup() {
        return groupRepository.findByName("testGroupForUWLs")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .name("testGroupForUWLs")
                                .build())
                );
    }

    /**
     * TODO.
     */
    Group getTestGroup(Tenant t, List<User> users) {
        return groupRepository.findByName("testGroupForUWLs")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(t)
                                .users(users)
                                .name("testGroupForUWLs")
                                .build())
                );
    }

    /**
     * TODO.
     */
    Group getTestGroup(List<User> users) {
        return groupRepository.findByName("testGroupForUWLs")
                .orElseGet(() -> groupRepository.save(
                        Group.builder()
                                .wearableGroup(getTestWearableGroup())
                                .tenant(getTestTenant())
                                .users(users)
                                .name("testGroupForUWLs")
                                .build())
                );
    }

    /**
     * TODO.
     */
    @BeforeEach
    void setUp() {
        getTestTenant();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        getTestUser();
    }

    @Test
    void testGetUserWearableLinksForUser() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime beginTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        UserWearableLink userWearableLink = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(beginTime).endTime(endTime).build();
        wearableRepository.save(wearable);
        userWearableLink = userWearableLinkRepository.save(userWearableLink);
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT + "/users/" + user.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        // then
        List<UserWearableLink> uwlsFromResponse =
                mapper.readValue(response.getBody(), new TypeReference<List<UserWearableLink>>() {
                });
        assertEquals(1, uwlsFromResponse.size());
        UserWearableLink uwlFromResponse = uwlsFromResponse.get(0);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertThat(uwlFromResponse)
                .usingRecursiveComparison()
                .ignoringFields("user")
                .ignoringFields("wearable")
                .isEqualTo(userWearableLink);
        assertEquals(user.getId(), uwlFromResponse.getUser().getId());
        assertEquals(wearable.getId(), uwlFromResponse.getWearable().getId());
    }

    @Test
    void testGetUserWearableLinksForWearable() throws JsonProcessingException {
        //given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime beginTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime endTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
        UserWearableLink userWearableLink = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(beginTime).endTime(endTime).build();
        userWearableLink = userWearableLinkRepository.save(userWearableLink);
        //when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/wearables/" + wearable.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        //then
        List<UserWearableLink> uwlsFromResponse = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        assertEquals(1, uwlsFromResponse.size());
        UserWearableLink uwlFromResponse = uwlsFromResponse.get(0);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertThat(uwlFromResponse)
                .usingRecursiveComparison()
                .ignoringFields("user")
                .ignoringFields("wearable")
                .isEqualTo(userWearableLink);
        assertEquals(user.getId(), uwlFromResponse.getUser().getId());
        assertEquals(wearable.getId(), uwlFromResponse.getWearable().getId());
    }

    @Test
    void testGetUserWearableLinksForTimeWindow() throws JsonProcessingException {
        //given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        LocalDateTime ldt = LocalDateTime.now();
        long beginTime = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTime = ldt.plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();
        LocalDateTime bTime = Instant.ofEpochMilli(beginTime).atZone(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime eTime = Instant.ofEpochMilli(endTime).atZone(ZoneOffset.UTC).toLocalDateTime();
        // outside matching window
        UserWearableLink leftOfMatchingWindow = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(bTime.minusDays(2)).endTime(bTime.minusDays(1)).build();
        UserWearableLink rightOfMatchingWindow = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(eTime.plusDays(1)).endTime(eTime.plusDays(2)).build();
        // (partially) inside matching window
        UserWearableLink equalToWindow = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(bTime).endTime(eTime).build();
        UserWearableLink fullyInsideWindow = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(bTime.plusSeconds(1)).endTime(eTime.minusSeconds(1)).build();
        UserWearableLink beginTimeInsideWindow = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(eTime.minusSeconds(1)).endTime(eTime.plusDays(1)).build();
        UserWearableLink endTimeInsideWindow = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .side(Wearable.Side.RIGHT).beginTime(bTime.minusDays(2)).endTime(eTime.minusSeconds(1)).build();

        leftOfMatchingWindow = userWearableLinkRepository.save(leftOfMatchingWindow);
        rightOfMatchingWindow = userWearableLinkRepository.save(rightOfMatchingWindow);
        equalToWindow = userWearableLinkRepository.save(equalToWindow);
        fullyInsideWindow = userWearableLinkRepository.save(fullyInsideWindow);
        beginTimeInsideWindow = userWearableLinkRepository.save(beginTimeInsideWindow);
        endTimeInsideWindow = userWearableLinkRepository.save(endTimeInsideWindow);
        List<Long> idsMatchingWindow = List.of(
                equalToWindow.getId(),
                fullyInsideWindow.getId(),
                beginTimeInsideWindow.getId(),
                endTimeInsideWindow.getId()
        );
        //when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT + String.format(
                        "?begin=%d&end=%d",
                        beginTime,
                        endTime
                ), getPort()), HttpMethod.GET, entity, String.class);
        //then
        List<UserWearableLink> uwlsFromResponse = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        List<Long> uwlIdsFromResponse = uwlsFromResponse.stream()
                .map(UserWearableLink::getId)
                .collect(Collectors.toUnmodifiableList());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(uwlsFromResponse.isEmpty());
        assertTrue(uwlIdsFromResponse.containsAll(idsMatchingWindow));
        assertFalse(uwlIdsFromResponse.contains(leftOfMatchingWindow.getId()));
        assertFalse(uwlIdsFromResponse.contains(rightOfMatchingWindow.getId()));
    }

    @Test
    void testLinkActiveUser() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable activeWearable = getTestWearable();
        Wearable wearableToLink = getOtherTestWearable();

        UserWearableLink active = UserWearableLink.builder()
                .user(user)
                .wearable(activeWearable)
                .beginTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.of(9999, 12, 31, 23, 59))
                .build();
        userWearableLinkRepository.save(active);
        UserWearableLinkForm form = new UserWearableLinkForm(user.getId(), wearableToLink.getId());
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT, getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        UserWearableLink uwl = mapper.readValue(response.getBody(), UserWearableLink.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(wearableToLink.getId(), uwl.getWearable().getId());
        assertEquals(user.getId(), uwl.getUser().getId());

        // and then when
        entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT + "/users/" + user.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        // then
        List<UserWearableLink> uwlsFromResponse = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        assertEquals(2, uwlsFromResponse.size());
        assertEquals(active.getWearable().getId(), uwlsFromResponse.get(0).getWearable().getId());
        assertEquals(uwl.getWearable().getId(), uwlsFromResponse.get(1).getWearable().getId());
        assertFalse(uwlsFromResponse.get(0).isActive());
        assertTrue(uwlsFromResponse.get(1).isActive());
        assertTrue(uwlsFromResponse.get(1).getBeginTime().isAfter(uwlsFromResponse.get(0).getEndTime()));
    }

    @Test
    void testLinkInactiveWearable() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        UserWearableLinkForm form = new UserWearableLinkForm(user.getId(), wearable.getId());
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT, getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        UserWearableLink uwl = mapper.readValue(response.getBody(), UserWearableLink.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(wearable.getId(), uwl.getWearable().getId());
        assertEquals(user.getId(), uwl.getUser().getId());
    }

    @Test
    void testLinkSameWearableToSameUser() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        UserWearableLinkForm form = new UserWearableLinkForm(user.getId(), wearable.getId());
        UserWearableLink active = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .beginTime(LocalDateTime.now())
                .endTime(LocalDateTime.of(9999, 12, 31, 23, 59))
                .side(Wearable.Side.RIGHT).build();
        userWearableLinkRepository.save(active);
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT, getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        UserWearableLink uwl = mapper.readValue(response.getBody(), UserWearableLink.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                uwl.getBeginTime().truncatedTo(ChronoUnit.MILLIS),
                active.getBeginTime().truncatedTo(ChronoUnit.MILLIS)
        );
        assertEquals(
                uwl.getEndTime().truncatedTo(ChronoUnit.MILLIS),
                active.getEndTime().truncatedTo(ChronoUnit.MILLIS)
        );
    }

    @Test
    void testLinkActiveWearable() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        User userToLinkTo = User.builder().tenant(tenant).authId("testUserGet2").build();
        user = userRepository.save(user);
        userToLinkTo = userRepository.save(userToLinkTo);
        Group group = getTestGroup(List.of(user, userToLinkTo));
        Wearable wearable = getTestWearable();
        UserWearableLinkForm form = new UserWearableLinkForm(userToLinkTo.getId(), wearable.getId());
        UserWearableLink active = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .beginTime(LocalDateTime.now())
                .endTime(LocalDateTime.of(9999, 12, 31, 23, 59))
                .side(Wearable.Side.RIGHT).build();
        userWearableLinkRepository.save(active);
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT, getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testUnlinkInactiveWearable() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        UnlinkActiveWearableForm form = new UnlinkActiveWearableForm(wearable.getId());
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT + "/wearables/unlink", getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testUnlinkActiveWearable() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        UnlinkActiveWearableForm form = new UnlinkActiveWearableForm(wearable.getId());
        UserWearableLink active = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .beginTime(LocalDateTime.now())
                .endTime(LocalDateTime.of(9999, 12, 31, 23, 59))
                .side(Wearable.Side.RIGHT).build();
        userWearableLinkRepository.save(active);
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT + "/wearables/unlink", getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        UserWearableLink uwl = mapper.readValue(response.getBody(), UserWearableLink.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(wearable.getId(), uwl.getWearable().getId());
        assertEquals(user.getId(), uwl.getUser().getId());
    }

    @Test
    void testUnlinkActiveUser() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        UnlinkActiveUserForm form = new UnlinkActiveUserForm(user.getId());
        UserWearableLink active = UserWearableLink.builder()
                .user(user).wearable(wearable)
                .beginTime(LocalDateTime.now())
                .endTime(LocalDateTime.of(9999, 12, 31, 23, 59))
                .side(Wearable.Side.RIGHT).build();
        userWearableLinkRepository.save(active);
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT + "/users/unlink", getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        UserWearableLink uwl = mapper.readValue(response.getBody(), UserWearableLink.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(wearable.getId(), uwl.getWearable().getId());
        assertEquals(user.getId(), uwl.getUser().getId());
    }

    @Test
    void testUnlinkInactiveUser() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        UnlinkActiveUserForm form = new UnlinkActiveUserForm(user.getId());
        // when
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT + "/users/unlink", getPort()),
                HttpMethod.POST, entity, String.class
        );
        // then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testGetActiveWearableLinksForGroup() throws JsonProcessingException {
        // given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Tenant tenant = getTestTenant();
        User firstUser = User.builder().tenant(tenant).authId("firstUser").build();
        firstUser = userRepository.save(firstUser);
        User secondUser = User.builder().tenant(tenant).authId("secondUser").build();
        secondUser = userRepository.save(secondUser);
        User thirdUser = User.builder().tenant(tenant).authId("thirdUser").build();
        thirdUser = userRepository.save(thirdUser);
        Group group = getTestGroup(List.of(firstUser, secondUser, thirdUser));
        Wearable firstWearable = getTestWearable();
        Wearable secondWearable = getOtherTestWearable();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beginTime = now.atOffset(ZoneOffset.UTC).minusDays(2).toLocalDateTime();
        UserWearableLink firstUwl = UserWearableLink.builder()
                .user(firstUser).wearable(firstWearable)
                .side(Wearable.Side.RIGHT).beginTime(beginTime).endTime(now.minusDays(1)).build(); // ended
        UserWearableLink secondUwl = UserWearableLink.builder()
                .user(secondUser).wearable(firstWearable)
                .side(Wearable.Side.RIGHT).beginTime(beginTime).endTime(now.plusDays(1)).build(); // still active
        UserWearableLink thirdUwl = UserWearableLink.builder()
                .user(thirdUser).wearable(secondWearable)
                .side(Wearable.Side.RIGHT).beginTime(beginTime).endTime(now.plusDays(5)).build(); // still active
        List<UserWearableLink> activeLinks = List.of(secondUwl, thirdUwl);
        userWearableLinkRepository.save(firstUwl);
        secondUwl = userWearableLinkRepository.save(secondUwl);
        thirdUwl = userWearableLinkRepository.save(thirdUwl);
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/active/groups/" + group.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        // then
        List<UserWearableLink> uwlsFromResponse = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        assertEquals(HttpStatus.OK, response.getStatusCode());
        /* TODO: Brittle (test var content dependent) way to check whether the proper UWLs are returned.
            However, the recursive comparison using assertThat kept failing equality. Should look into this. */
        assertEquals(2, uwlsFromResponse.size());
        assertEquals(activeLinks.get(0).getWearable().getId(), uwlsFromResponse.get(0).getWearable().getId());
        assertEquals(activeLinks.get(1).getWearable().getId(), uwlsFromResponse.get(1).getWearable().getId());
    }

    @Test
    void testGetLinksForTenant() throws JsonProcessingException {
        // given
        AccessScopeContext.INSTANCE.setTenantId(2L);
        Tenant tenant = tenantRepository.findById(2L).get();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(tenant, List.of(user));
        Wearable wearable = getTestWearable();
        LocalDateTime beginTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        UserWearableLink uwl = UserWearableLink.builder()
                .user(user)
                .wearable(wearable)
                .beginTime(beginTime)
                .endTime(endTime)
                .side(Wearable.Side.RIGHT)
                .build();
        userWearableLinkRepository.save(uwl);
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());
        assertTrue(userWearableLinkRepository.findAll().isEmpty()); // no uwl's for default test tenant
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        String uri = String.format(
                "%s?begin=%d&end=%d",
                USER_WEARABLE_LINKS_REST_ENDPOINT,
                beginTime.toEpochSecond(ZoneOffset.UTC),
                endTime.toEpochSecond(ZoneOffset.UTC)
        );
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(uri, getPort()), HttpMethod.GET, entity, String.class);
        List<UserWearableLink> uwls = mapper.readValue(response.getBody(), new TypeReference<List<UserWearableLink>>() {
        });
        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(uwls.isEmpty());
    }

    @Test
    void testLinkArchivedUser() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();

        UserWearableLink uwl = UserWearableLink.builder()
                .user(user)
                .wearable(wearable)
                .beginTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.of(9999, 12, 31, 23, 59))
                .build();
        uwl = userWearableLinkRepository.save(uwl);
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/active/groups/" + group.getId(), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        // then
        List<UserWearableLink> activeUWLs = mapper.readValue(response.getBody(), List.class);
        assertEquals(1, activeUWLs.size());
        // and then when: we archive the user
        getRestTemplate().exchange(
                TestUtils.createURLWithPort("/users/" + user.getId() + "/archive", getPort()),
                HttpMethod.PATCH,
                entity,
                String.class
        );
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/active/groups/" + group.getId(), getPort()),
                HttpMethod.GET,
                entity,
                String.class
        );
        // then: no active links should exist
        activeUWLs = mapper.readValue(response.getBody(), List.class);
        assertEquals(0, activeUWLs.size());
        // and then when: we try to create a new link
        UserWearableLinkForm form = new UserWearableLinkForm(user.getId(), wearable.getId());
        entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(USER_WEARABLE_LINKS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        // then: user is archived exception is thrown
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testGetActiveLinkOfWearable() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/active/wearables/" + wearable.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // then given
        UserWearableLink uwl = UserWearableLink.builder()
                .user(user)
                .wearable(wearable)
                .beginTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .build();
        userWearableLinkRepository.save(uwl);
        // when
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/active/wearables/" + wearable.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        UserWearableLink uwlResp = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        // then
        assertNotNull(uwlResp);
    }

    @Test
    void testGetActiveLinkOfUser() throws JsonProcessingException {
        // given
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/active/users/" + user.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // then given
        UserWearableLink uwl = UserWearableLink.builder()
                .user(user)
                .wearable(wearable)
                .beginTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .build();
        userWearableLinkRepository.save(uwl);
        // when
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/active/users/" + user.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        UserWearableLink uwlResp = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        // then
        assertNotNull(uwlResp);
    }

    @Test
    void testIsWearableLinked() throws JsonProcessingException {
        // given: A wearable with no links
        Tenant tenant = getTestTenant();
        User user = User.builder().tenant(tenant).authId("testUserGet").build();
        user = userRepository.save(user);
        Group group = getTestGroup(List.of(user));
        Wearable wearable = getTestWearable();
        // when
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/is-active/wearables/" + wearable.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        Boolean isLinked = mapper.readValue(response.getBody(), new TypeReference<Boolean>() {
        });
        // then
        assertFalse(isLinked);

        // then given: The wearable gets an active link
        UserWearableLink uwl = UserWearableLink.builder()
                .wearable(wearable)
                .user(user)
                .beginTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(1))
                .build();
        userWearableLinkRepository.save(uwl);
        // then when
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        USER_WEARABLE_LINKS_REST_ENDPOINT + "/is-active/wearables/" + wearable.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        isLinked = mapper.readValue(response.getBody(), new TypeReference<Boolean>() {
        });
        // then
        assertTrue(isLinked);
    }
}
