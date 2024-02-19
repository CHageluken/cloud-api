package smartfloor.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.repository.jpa.FallRiskProfileRepository;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class FallRiskProfileServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private FallRiskProfileRepository fallRiskProfileRepository;

    @InjectMocks
    private FallRiskProfileService fallRiskProfileService;

    @Test
    void testGetFallRiskProfilesForWearableIdWithinTimeWindow() {
        // given
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        Wearable wearable = Wearable.builder().id("test_heelable").build();
        TimeWindow timeWindow = new TimeWindow(currentTime.minusDays(1), currentTime);
        FallRiskProfile frp = FallRiskProfile.builder().wearable(wearable).build();
        List<FallRiskProfile> fallRiskProfiles = new ArrayList<>();
        fallRiskProfiles.add(frp);
        // when
        Mockito.when(fallRiskProfileRepository.findByWearableIdAndCreationTimeBetweenAndHiddenFalseOrderByCreationTime(
                wearable.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        )).thenReturn(fallRiskProfiles);
        // then
        List<FallRiskProfile> foundFallRiskProfiles =
                fallRiskProfileService.getFallRiskProfilesForWearableWithinTimeWindow(wearable, timeWindow);
        assertEquals(foundFallRiskProfiles, fallRiskProfiles);
    }
}
