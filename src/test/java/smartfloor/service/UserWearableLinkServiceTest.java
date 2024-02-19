package smartfloor.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.UserWearableLinkNotFoundException;
import smartfloor.domain.exception.WearableInUseException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.repository.jpa.UserWearableLinkRepository;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class UserWearableLinkServiceTest {

    @Mock
    private UserWearableLinkRepository userWearableLinkRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private UserWearableLinkService userWearableLinkService;

    private UserWearableLink buildUserWearableLink(Long userWearableLinkId, String wearableId, Long userId) {
        UserWearableLink userWearableLink = new UserWearableLink();
        Wearable wearable = Wearable.builder()
                .id(wearableId)
                .userWearableLinks(new ArrayList<>())
                .userWearableLinks(new ArrayList<>())
                .build();
        User user = new User();
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        user.setId(userId);
        userWearableLink.setId(userWearableLinkId);
        userWearableLink.setWearable(wearable);
        userWearableLink.setUser(user);
        userWearableLink.setBeginTime(currentTime.minusDays(1));
        userWearableLink.setEndTime(currentTime);
        userWearableLink.setSide(Wearable.Side.RIGHT);
        return userWearableLink;
    }

    @Test
    void testCreateUserWearableLinkWithExistingActiveOne()
            throws WearableInUseException, UserIsArchivedException {
        // given
        UserWearableLink userWearableLink = buildUserWearableLink(null, "test_wearable", 1L);
        UserWearableLink active = buildUserWearableLink(null, "test_wearable", 1L);
        // when
        Mockito.when(userWearableLinkRepository.findLatestByWearableIdWithOverlappingTimeWindow(
                        userWearableLink.getWearable().getId(),
                        userWearableLink.getBeginTime(),
                        userWearableLink.getEndTime()
                )
        ).thenReturn(Optional.empty());
        userWearableLinkService.createUserWearableLink(userWearableLink);
        // then
        Mockito.verify(userWearableLinkRepository, never())
                .delete(any(UserWearableLink.class));
        Mockito.verify(userWearableLinkRepository, times(1))
                .saveAndFlush(userWearableLink);
        Mockito.verify(authorizationService, atLeast(1))
                .validateUserOperationAuthority(userWearableLink.getUser());
        assertTrue(userWearableLink.getBeginTime()
                .isAfter(active.getEndTime()));
    }

    @Test
    void testCreateOverlappingUserWearableLink() {
        // given
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        UserWearableLink overlappingLink = buildUserWearableLink(1L, "test_wearable_link", 1L);
        UserWearableLink newLink = buildUserWearableLink(null, "test_wearable_link", 2L);
        newLink.setBeginTime(currentTime.minusDays(2));
        newLink.setEndTime(currentTime);
        // when
        Mockito.when(userWearableLinkRepository.findLatestByWearableIdWithOverlappingTimeWindow(newLink.getWearable()
                .getId(), newLink.getBeginTime(), newLink.getEndTime())).thenReturn(Optional.of(overlappingLink));
        // then already in use exception is thrown
        assertThrows(WearableInUseException.class, () -> userWearableLinkService.createUserWearableLink(newLink));
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(newLink.getUser());
    }

    @Test
    void testCreateOverlappingUserWearableLinkWithSameUser()
            throws WearableInUseException, UserIsArchivedException {
        // given
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        UserWearableLink overlappingLink = buildUserWearableLink(1L, "test_wearable_overlap", 1L);
        UserWearableLink newLink = buildUserWearableLink(null, "test_wearable_link", 1L);
        newLink.setBeginTime(currentTime.minusDays(2));
        newLink.setEndTime(currentTime);
        // when
        Mockito.when(userWearableLinkRepository.findLatestByWearableIdWithOverlappingTimeWindow(newLink.getWearable()
                .getId(), newLink.getBeginTime(), newLink.getEndTime())).thenReturn(Optional.of(overlappingLink));
        UserWearableLink uwl = userWearableLinkService.createUserWearableLink(newLink);
        // then
        assertEquals(uwl, overlappingLink);
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(newLink.getUser());
    }

    @Test
    void testCreateNonOverlappingUserWearableLink()
            throws UserNotFoundException, WearableNotFoundException, WearableInUseException, UserIsArchivedException {
        // given
        UserWearableLink userWearableLink = buildUserWearableLink(null, "test_wearable", 1L);
        // when
        Mockito.when(userWearableLinkRepository.findLatestByWearableIdWithOverlappingTimeWindow(
                        userWearableLink.getWearable().getId(),
                        userWearableLink.getBeginTime(),
                        userWearableLink.getEndTime()
                )
        ).thenReturn(Optional.empty());
        userWearableLinkService.createUserWearableLink(userWearableLink);
        // then
        Mockito.verify(userWearableLinkRepository, never()).delete(any(UserWearableLink.class));
        Mockito.verify(userWearableLinkRepository, times(1)).saveAndFlush(userWearableLink);
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(userWearableLink.getUser());
    }

    @Test
    void testGetByUserId() throws UserWearableLinkNotFoundException {
        // given
        Long userId = 1L;
        UserWearableLink userWearableLink = buildUserWearableLink(userId, "test_wearable", 1L);
        List<UserWearableLink> byUserId = new ArrayList<>();
        byUserId.add(userWearableLink);
        // when
        Mockito.when(userWearableLinkRepository.findByUserIdOrderByBeginTimeAsc(userId, UserWearableLink.class))
                .thenReturn(byUserId);
        List<UserWearableLink> actual = userWearableLinkService.getByUserId(userId);
        // then
        assertNotNull(actual);
        assertTrue(actual.contains(byUserId.get(0)));
        Mockito.verify(authorizationService, atLeast(1))
                .validateUserOperationAuthority(userWearableLink.getUser().getId());
    }

    @Test
    void testGetByWearableId() {
        // given
        String wearableId = "test_wearable";
        UserWearableLink userWearableLink = buildUserWearableLink(1L, wearableId, 1L);
        List<UserWearableLink> byWearableId = new ArrayList<>();
        byWearableId.add(userWearableLink);
        // when
        Mockito.when(userWearableLinkRepository.findByWearableId(wearableId, UserWearableLink.class))
                .thenReturn(byWearableId);
        List<UserWearableLink> actual = userWearableLinkService.getByWearableId(wearableId);
        // then
        assertNotNull(actual);
        assertTrue(actual.contains(byWearableId.get(0)));
    }

    @Test
    void testGetNonExistingLinkByUserId() {
        // given
        Long nonExistingLinksUserId = 1L;
        // when
        Mockito.when(userWearableLinkRepository.findByUserIdOrderByBeginTimeAsc(
                nonExistingLinksUserId,
                UserWearableLink.class
        )).thenReturn(new ArrayList<>());
        List<UserWearableLink> actual = userWearableLinkService.getByUserId(nonExistingLinksUserId);
        // then
        assertTrue(actual.isEmpty());
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(nonExistingLinksUserId);
    }


    @Test
    void testGetNonExistingLinkByWearableId() {
        // given
        String nonExistingLinksWearableId = "test_wearable";
        // when
        Mockito.when(userWearableLinkRepository.findByWearableId(nonExistingLinksWearableId, UserWearableLink.class))
                .thenReturn(new ArrayList<>());
        List<UserWearableLink> actual = userWearableLinkService.getByWearableId(nonExistingLinksWearableId);
        // then
        assertTrue(actual.isEmpty());
    }

    @Test
    void testDeleteUserWearableLink() {
        // given
        Wearable wearable = Wearable.builder().id("test_wearable").build();
        User user = User.builder().authId("test_user").tenant(Tenant.getDefaultTenant()).build();
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime beginTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime();
        UserWearableLink userWearableLink = UserWearableLink.builder()
                .side(Wearable.Side.RIGHT).beginTime(beginTime).endTime(endTime)
                .user(user).wearable(wearable).build();
        // when
        userWearableLinkService.deleteUserWearableLink(userWearableLink);
        // then
        Mockito.verify(userWearableLinkRepository, times(1)).delete(userWearableLink);
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(userWearableLink.getUser());
    }

    @Test
    void testGetByUserIdWithOverlappingTimeWindow() throws UserWearableLinkNotFoundException {
        //given
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime beginTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime();
        TimeWindow timeWindow = new TimeWindow();
        timeWindow.setBeginTime(beginTime);
        timeWindow.setEndTime(endTime);
        Long userId = 1L;
        User user = User.builder().build();
        user.setId(userId);
        UserWearableLink userWearableLink = buildUserWearableLink(1L, "test_wearable", userId);
        List<UserWearableLink> byUserId = new ArrayList<>();
        byUserId.add(userWearableLink);
        //when
        Mockito.when(userWearableLinkRepository.findAllByUserIdWithOverlappingTimeWindow(
                userId,
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        )).thenReturn(byUserId);
        List<UserWearableLink> actual = userWearableLinkService.getByUserWithOverlappingTimeWindow(user, timeWindow);
        //then
        assertNotNull(actual);
        assertTrue(actual.contains((byUserId.get(0))));
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(userWearableLink.getUser());
    }

    @Test
    void testGetByTimeWindow() throws UserWearableLinkNotFoundException {
        //given
        UserWearableLink userWearableLink = buildUserWearableLink(1L, "test_wearable", 1L);
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime beginTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endTime = ldt.atZone(ZoneOffset.UTC).toLocalDateTime();
        TimeWindow timeWindow = new TimeWindow();
        timeWindow.setBeginTime(beginTime);
        timeWindow.setEndTime(endTime);
        List<UserWearableLink> byTimeWindow = new ArrayList<>();
        byTimeWindow.add(userWearableLink);
        //when
        Mockito.when(userWearableLinkRepository.findAllByOverlappingTimeWindow(beginTime, endTime))
                .thenReturn(byTimeWindow);
        List<UserWearableLink> actual = userWearableLinkService.getByOverlappingTimeWindow(timeWindow);
        //then
        assertTrue(actual.contains(byTimeWindow.get(0)));
    }

    @Test
    void testLinkArchivedUser() throws WearableInUseException, UserIsArchivedException {
        // given
        User user = User.builder().build();
        user.setId(1L);
        user.archive();
        UserWearableLink userWearableLink = buildUserWearableLink(1L, "test_wearable", user.getId());
        userWearableLink.setUser(user);
        // when, then user is archived exception is thrown
        assertThrows(
                UserIsArchivedException.class,
                () -> userWearableLinkService.createUserWearableLink(userWearableLink)
        );
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(userWearableLink.getUser());
    }
}
