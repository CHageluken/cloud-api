package smartfloor.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Position;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.UserWearableLinkNotFoundException;
import smartfloor.repository.jpa.FootstepRepository;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class FootstepServiceTest {

    @Mock
    private FootstepRepository footstepRepository;

    @Mock
    private UserWearableLinkService userWearableLinkService;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private FootstepService footstepService;

    private Random rand;

    @BeforeEach
    void setUp() {
        rand = new Random();
    }

    /**
     * Test to see if a footstep is added by checking to see if the save method of the repository is called for the
     * footstep object.
     */
    @Test
    void testAddFootstep() {
        // given
        Footstep footstep = new Footstep();
        // when
        footstepService.addFootstep(footstep);
        // then
        Mockito.verify(footstepRepository, times(1)).save(footstep);
    }

    /**
     * Test to see if we get a valid list of footsteps for the provided wearable.
     */
    @Test
    void testGetByWearableWithinTimeWindow() {
        // given
        TimeWindow timeWindow = new TimeWindow();
        timeWindow.setBeginTime(LocalDateTime.now().minusMinutes(5));
        timeWindow.setEndTime(LocalDateTime.now());
        Wearable wearable = new Wearable();
        wearable.setId("test_wearable");
        int amountOfFootsteps = 50;
        List<Footstep> footsteps = buildRandomListOfFootsteps(amountOfFootsteps, wearable);
        // when
        Mockito.when(footstepRepository.findByWearableIdAndTimeBetweenOrderByTimeAsc(
                wearable.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        )).thenReturn(footsteps);
        List<Footstep> actual = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        // then
        assertNotNull(actual);
        assertEquals(footsteps, actual);
    }

    /**
     * Test to see if we get a valid number of footsteps for the provided wearable.
     */
    @Test
    void testGetCountByWearableWithinTimeWindow() {
        // given
        TimeWindow timeWindow = new TimeWindow();
        timeWindow.setBeginTime(LocalDateTime.now().minusMinutes(5));
        timeWindow.setEndTime(LocalDateTime.now());
        Wearable wearable = new Wearable();
        wearable.setId("test_wearable");
        int amountOfFootsteps = 50;
        List<Footstep> footsteps = buildRandomListOfFootsteps(amountOfFootsteps, wearable);
        // when
        Mockito.when(footstepRepository.countByWearableIdAndTimeBetween(
                wearable.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        )).thenReturn(amountOfFootsteps);
        int actual = footstepService.getCountByWearableWithinTimeWindow(wearable, timeWindow);
        // then
        assertEquals(amountOfFootsteps, actual);
    }

    private List<Footstep> buildRandomListOfFootsteps(int amount, Wearable madeByWearable) {
        List<Footstep> footsteps = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Footstep footstep = new Footstep();
            footstep.setPosition(new Position(rand.nextInt(50), rand.nextInt(50)));
            footstep.setWearable(madeByWearable);
            footsteps.add(footstep);
        }
        return footsteps;
    }

    @Test
    void testGetSortedFootsteps() throws UserNotFoundException, UserWearableLinkNotFoundException {
        Wearable wearable = new Wearable();
        wearable.setId("testWearable");
        UserWearableLink userWearableLink = new UserWearableLink();
        userWearableLink.setWearable(wearable);
        userWearableLink.setBeginTime(LocalDateTime.now().minusDays(1));
        userWearableLink.setEndTime(LocalDateTime.now());
        List<UserWearableLink> userWearableLinks = new ArrayList<>();
        userWearableLinks.add(userWearableLink);

        User user = new User();
        user.setId(1L);

        Footstep footstep1 = new Footstep();
        footstep1.setPosition(new Position(50, 50));
        footstep1.setTime(LocalDateTime.now().minusSeconds(2));
        footstep1.setWearable(wearable);

        Footstep footstep2 = new Footstep();
        footstep2.setPosition(new Position(200, 200));
        footstep2.setTime(LocalDateTime.now());
        footstep2.setWearable(wearable);

        TimeWindow timeWindow = new TimeWindow(footstep1.getTime(), footstep2.getTime());

        List<Footstep> footsteps = new ArrayList<>();
        footsteps.add(footstep1);
        footsteps.add(footstep2);

        Mockito.when(userWearableLinkService.getByUserWithOverlappingTimeWindow(user, timeWindow))
                .thenReturn(userWearableLinks);
        Mockito.when(footstepRepository.findByWearableIdAndTimeBetweenOrderByTimeAsc(
                wearable.getId(),
                userWearableLink.getBeginTime(),
                userWearableLink.getEndTime()
        )).thenReturn(footsteps);
        assertEquals(footsteps, footstepService.getForUserWithinTimeWindow(user, timeWindow));
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user);
    }

    /**
     * Test to see if the Euclidean distance value for two predefined footsteps with a random position correctly return
     * the Euclidean distance between them. Covers the first branch of the service method.
     */
    @Test
    void testGetEuclideanDistanceBetweenFootsteps() {
        // given
        Footstep m1 = new Footstep();
        Footstep m2 = new Footstep();
        Position randomPosition = new Position(rand.nextInt(50), rand.nextInt(50));
        Position randomPosition2 = new Position(rand.nextInt(50), rand.nextInt(50));
        m1.setPosition(randomPosition);
        m2.setPosition(randomPosition2);
        // when
        double xDelta = Math.abs(m1.getPosition().getX() - m2.getPosition().getX());
        double yDelta = Math.abs(m1.getPosition().getY() - m2.getPosition().getY());
        double euclideanDistanceBetweenFootsteps = Math.sqrt((xDelta * xDelta) + (yDelta * yDelta));

        double actual = FootstepService.getEuclideanDistanceBetweenFootsteps(m1, m2);
        // then
        assertEquals(euclideanDistanceBetweenFootsteps, actual, 0.0);
    }

    /**
     * Test to see if the Euclidean distance value for two empty footsteps (containing no position at all) correctly
     * returns 0.0. Covers the second branch of the service method.
     */
    @Test
    void testGetEuclideanDistanceBetweenEmptyFootsteps() {
        // given
        Footstep m1 = new Footstep();
        Footstep m2 = new Footstep();
        assertNull(m1.getPosition());
        assertNull(m2.getPosition());
        // when
        double actual = FootstepService.getEuclideanDistanceBetweenFootsteps(m1, m2);
        // then
        assertEquals(0.0, actual, 0.0);
    }
}
