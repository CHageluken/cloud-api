package smartfloor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskScoreAssessment;
import smartfloor.domain.entities.fall.risk.profile.LatestFallRiskProfileAssessment;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class FallRiskAnalysisServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private FallRiskProfileService fallRiskProfileService;

    @InjectMocks
    private FallRiskAnalysisService fallRiskAnalysisService;

    /**
     * Test case that asserts that we get the correct (that is, latest and best) fall risk assessment
     * from the computeLatestBestFallRiskAssessmentForUser method.
     */
    @Test
    void testComputeLatestBestFallRiskAssessmentForUser() {
        // given
        User user = User.builder().authId("test").build();
        user.setId(1L);

        LocalDateTime a = LocalDateTime.now().minusDays(2);
        LocalDateTime b = LocalDateTime.now().minusDays(1);
        LocalDateTime c = LocalDateTime.now();
        TimeWindow ab = new TimeWindow(a, b);
        TimeWindow bc = new TimeWindow(b, c);
        TimeWindow ac = new TimeWindow(a, c);

        FallRiskProfile oldestBest = FallRiskProfile.builder()
                .walkingSpeed(100.0)
                .stepLength(50.0)
                .stepFrequency(1.0)
                .endTime(a)
                .build();
        FallRiskProfile newerWorse = FallRiskProfile.builder()
                .walkingSpeed(50.0)
                .stepLength(25.0)
                .stepFrequency(1.0)
                .endTime(a)
                .build();
        FallRiskProfile newerBest = FallRiskProfile.builder()
                .walkingSpeed(100.0)
                .stepLength(50.0)
                .stepFrequency(1.0)
                .endTime(b)
                .build();
        FallRiskProfile latestWorse = FallRiskProfile.builder()
                .walkingSpeed(50.0)
                .stepLength(25.0)
                .stepFrequency(1.0)
                .endTime(c)
                .build();
        FallRiskProfile latestBest = FallRiskProfile.builder()
                .walkingSpeed(100.0)
                .stepLength(50.0)
                .stepFrequency(1.0)
                .endTime(c)
                .build();

        assertTrue(latestWorse.getEndTime().toLocalDate().isAfter(oldestBest.getEndTime().toLocalDate()));
        assertTrue(latestBest.getEndTime().toLocalDate().isAfter(oldestBest.getEndTime().toLocalDate()));
        assertTrue(latestWorse.getEndTime().toLocalDate().isEqual(latestBest.getEndTime().toLocalDate()));

        FallRiskScoreAssessment newerBestFrsa = new FallRiskScoreAssessment(newerBest);
        FallRiskScoreAssessment latestWorstFrsa = new FallRiskScoreAssessment(latestWorse);
        FallRiskScoreAssessment latestBestFrsa = new FallRiskScoreAssessment(latestBest);

        assertEquals(newerBestFrsa.getTotal(), latestBestFrsa.getTotal(), 0.0);
        assertTrue(latestWorstFrsa.getTotal() < latestBestFrsa.getTotal());

        // when
        Mockito.when(fallRiskProfileService.getFallRiskProfilesForUserWithinTimeWindow(user, ab))
                .thenReturn(List.of(oldestBest, newerWorse, newerBest));

        Mockito.when(fallRiskProfileService.getFallRiskProfilesForUserWithinTimeWindow(user, bc))
                .thenReturn(List.of(newerBest, newerWorse, latestWorse, latestBest));

        Mockito.when(fallRiskProfileService.getFallRiskProfilesForUserWithinTimeWindow(user, ac))
                .thenReturn(List.of(oldestBest, newerWorse, latestWorse, latestBest, newerBest));

        // then when time window is ab
        LatestFallRiskProfileAssessment expected = new LatestFallRiskProfileAssessment(user, newerBestFrsa);
        Optional<LatestFallRiskProfileAssessment> latestFallRiskAssessmentInTimeWindowAB =
                fallRiskAnalysisService.computeLatestBestFallRiskAssessmentsForUserWithinTimeWindow(user, ab);

        assertTrue(latestFallRiskAssessmentInTimeWindowAB.isPresent());
        assertEquals(expected, latestFallRiskAssessmentInTimeWindowAB.get());

        // then when time window is bc
        expected = new LatestFallRiskProfileAssessment(user, latestBestFrsa);
        Optional<LatestFallRiskProfileAssessment> latestFallRiskAssessmentInTimeWindowBC =
                fallRiskAnalysisService.computeLatestBestFallRiskAssessmentsForUserWithinTimeWindow(user, bc);

        assertTrue(latestFallRiskAssessmentInTimeWindowBC.isPresent());
        assertEquals(expected, latestFallRiskAssessmentInTimeWindowBC.get());

        // then when time window is ac
        // `expected` does not change
        Optional<LatestFallRiskProfileAssessment> latestFallRiskAssessmentInTimeWindowAC =
                fallRiskAnalysisService.computeLatestBestFallRiskAssessmentsForUserWithinTimeWindow(user, ac);

        assertTrue(latestFallRiskAssessmentInTimeWindowAC.isPresent());
        assertEquals(expected, latestFallRiskAssessmentInTimeWindowAC.get());
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user);
    }

}
