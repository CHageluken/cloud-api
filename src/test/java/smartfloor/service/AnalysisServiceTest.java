package smartfloor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Position;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.AverageStrideFrequency;
import smartfloor.domain.indicators.footstep.AverageStrideLength;
import smartfloor.domain.indicators.footstep.CoveredDistance;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class AnalysisServiceTest {

    @Mock
    private FootstepService footstepService;

    @Mock
    private AuthorizationService authorizationService;

    /**
     * Service to test.
     */
    @InjectMocks
    private AnalysisService analysisService;

    /**
     * Get the covered distance based on a given set of footsteps for a user within a given time window.
     */
    @Test
    void testGetCoveredDistanceForUserWithinTimeWindow() {
        // given
        List<Footstep> footsteps = buildRandomListOfFootsteps(100);
        Long userId = 1L;
        User user = User.builder().build();
        user.setId(userId);
        TimeWindow timeWindow =
                new TimeWindow(footsteps.get(0).getTime(), footsteps.get(footsteps.size() - 1).getTime());
        CoveredDistance cd = CoveredDistance.of(footsteps);
        // when
        Mockito.when(footstepService.getForUserWithinTimeWindow(user, timeWindow)).thenReturn(footsteps);
        CoveredDistance actual = analysisService.getCoveredDistanceForUserWithinTimeWindow(user, timeWindow);
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user);
        assertEquals(cd.getValue(), actual.getValue(), 0.0);
    }

    /**
     * Get the average speed based on a given set of footsteps for a user within a given time window.
     */
    @Test
    void testGetAverageSpeedForUserWithinTimeWindow() {
        // given
        List<Footstep> footsteps = buildRandomListOfFootsteps(100);
        Long userId = 1L;
        User user = User.builder().build();
        user.setId(userId);
        TimeWindow timeWindow =
                new TimeWindow(footsteps.get(0).getTime(), footsteps.get(footsteps.size() - 1).getTime());
        AverageSpeed as = AverageSpeed.of(footsteps);
        // when
        Mockito.when(footstepService.getForUserWithinTimeWindow(user, timeWindow)).thenReturn(footsteps);
        AverageSpeed actual = analysisService.getAverageSpeedForUserWithinTimeWindow(user, timeWindow);
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user.getId());
        assertEquals(as.getValue(), actual.getValue(), 0.0);
    }

    /**
     * Get the average step length based on a given set of footsteps for a user within a given time window.
     */
    @Test
    void testGetAverageStepLengthForUserWithinTimeWindow() {
        // given
        List<Footstep> footsteps = buildRandomListOfFootsteps(100);
        Long userId = 1L;
        User user = User.builder().build();
        user.setId(userId);
        TimeWindow timeWindow =
                new TimeWindow(footsteps.get(0).getTime(), footsteps.get(footsteps.size() - 1).getTime());
        AverageStrideLength asl = AverageStrideLength.of(footsteps);
        // when
        Mockito.when(footstepService.getForUserWithinTimeWindow(user, timeWindow)).thenReturn(footsteps);
        AverageStrideLength actual = analysisService.getAverageStrideLengthForUserWithinTimeWindow(user, timeWindow);
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user);
        assertEquals(asl.getValue(), actual.getValue(), 0.0);
    }

    /**
     * Get the average step frequency based on a given set of footsteps for a user within a given time window.
     */
    @Test
    void testGetAverageStepFrequencyForUserWithinTimeWindow() {
        // given
        List<Footstep> footsteps = buildRandomListOfFootsteps(100);
        Long userId = 1L;
        User user = User.builder().build();
        user.setId(userId);
        TimeWindow timeWindow =
                new TimeWindow(footsteps.get(0).getTime(), footsteps.get(footsteps.size() - 1).getTime());
        AverageStrideFrequency asf = AverageStrideFrequency.of(footsteps);
        // when
        Mockito.when(footstepService.getForUserWithinTimeWindow(user, timeWindow)).thenReturn(footsteps);
        AverageStrideFrequency actual = analysisService.getStrideFrequencyForUserWithinTimeWindow(user, timeWindow);
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user);
        assertEquals(asf.getValue(), actual.getValue(), 0.0);
    }

    /**
     * Utility method to build a random list of footsteps for use in tests.
     */
    private List<Footstep> buildRandomListOfFootsteps(int amount) {
        List<Footstep> randomFootsteps = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Footstep randomFootstep = new Footstep();
            randomFootstep.setFloor(Floor.builder().name("Test").build());
            randomFootstep.setWearable(Wearable.builder().id("Test").build());
            Position randomPosition = new Position(
                    ThreadLocalRandom.current().nextInt(amount * 2),
                    ThreadLocalRandom.current().nextInt(amount * 2)
            );
            long minDay = LocalDate.of(1970, 1, 1).toEpochDay();
            long maxDay = LocalDate.of(2022, 12, 31).toEpochDay();
            long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
            if (i == 0) {
                randomFootstep.setTime(LocalDateTime.ofEpochSecond(minDay, 0, ZoneOffset.UTC));
            } else if (i == amount - 1) {
                randomFootstep.setTime(LocalDateTime.ofEpochSecond(maxDay, 0, ZoneOffset.UTC));
            } else {
                randomFootstep.setTime(LocalDateTime.ofEpochSecond(randomDay, 0, ZoneOffset.UTC));
            }
            randomFootstep.setPosition(randomPosition);
            randomFootsteps.add(randomFootstep);
        }
        return randomFootsteps;
    }

}
