package smartfloor.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.fall.risk.profile.FallRiskAssessmentModel;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskScoreAssessment;
import smartfloor.domain.entities.fall.risk.profile.LatestFallRiskProfileAssessment;
import smartfloor.domain.entities.fall.risk.profile.V1FallRiskScoreAssessment;

@Service
public class FallRiskAnalysisService {

    private final FootstepService footstepService;
    private final FallRiskProfileService fallRiskProfileService;
    private final AuthorizationService authorizationService;

    /**
     * TODO.
     */
    @Autowired
    public FallRiskAnalysisService(
            FootstepService footstepService,
            FallRiskProfileService fallRiskProfileService,
            AuthorizationService authorizationService
    ) {
        this.footstepService = footstepService;
        this.fallRiskProfileService = fallRiskProfileService;
        this.authorizationService = authorizationService;
    }

    /**
     * For every non-hidden fall risk profile (FRP) that we find for the user within the given time window,
     * we compute a fall risk assessment. The chosen fall risk assessment is the (currently used) score model.
     *
     * @param user       The user for which we want to compute fall risk assessments
     * @param timeWindow The time window for which we want to compute fall risk assessments
     * @return a list of fall risk (score) assessments for the user within the given time window
     */
    public List<FallRiskAssessmentModel> computeFallRiskAssessmentsForUserWithinTimeWindow(
            User user,
            TimeWindow timeWindow
    ) {
        authorizationService.validateUserOperationAuthority(user);

        return fallRiskProfileService.getFallRiskProfilesForUserWithinTimeWindow(user, timeWindow).stream()
                .map(FallRiskScoreAssessment::new)
                .collect(toList());
    }

    /**
     * Create a fall risk assessment for a given wearable and time window combination.
     */
    public FallRiskAssessmentModel computeFallRiskAssessmentForWearableWithinTimeWindow(
            Wearable wearable,
            TimeWindow timeWindow
    ) {
        List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        FallRiskProfile frp = FallRiskProfile.fromFootsteps(footsteps);
        return new FallRiskScoreAssessment(frp);
    }

    /**
     * For every hidden fall risk profile (FRP) that we find for the user, we compute a fall risk assessment.
     * The chosen fall risk assessment is the (currently used) score model.
     *
     * @return a list of fall risk (score) assessments for the user
     */
    public List<FallRiskAssessmentModel> computeHiddenFallRiskAssessmentsForUser(Long userId) {
        authorizationService.validateUserOperationAuthority(userId);

        return fallRiskProfileService.getFallRiskProfilesForUser(userId, true).stream()
                .map(FallRiskScoreAssessment::new)
                .collect(toList());
    }

    /**
     * TODO.
     */
    public List<FallRiskAssessmentModel> computeHiddenFallRiskAssessmentsForWearableWithinTimeWindow(
            Wearable wearable,
            TimeWindow timeWindow
    ) {
        return fallRiskProfileService.getFallRiskProfilesForWearableWithinTimeWindow(wearable, timeWindow, true)
                .stream()
                .map(FallRiskScoreAssessment::new)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * <p>Fetches the latest and best fall risk measurement for a given user based on their latest known fall risk
     * profile.</p>
     * Explanation: In some cases, we want to show the latest and best FRP for a user.
     * Showing the latest FRP is relatively easy, we can filter on end time and return the one with the maximum end
     * time. However, SF uses a weaker definition of latest in that it only needs to have the latest (maximum) date
     * (not time). There may be multiple such FRPs (having the latest date) but with different end times.
     * To pick between these FRPs, SF wants to show the "best" FRP where best is defined as having the highest total
     * score. This helper function filters out this latest and best FRP according to the aforementioned definitions.
     * <p>TODO: Implement an efficient version of fetching the latest measurement. Possible with a custom query method
     * in the repository that fetches it.</p>
     *
     * @return The latest FRP in terms of date that is the best for that day in terms of its total score.
     */
    public Optional<LatestFallRiskProfileAssessment> computeLatestBestFallRiskAssessmentsForUserWithinTimeWindow(
            User user,
            TimeWindow timeWindow
    ) {
        authorizationService.validateUserOperationAuthority(user);

        return fallRiskProfileService.getFallRiskProfilesForUserWithinTimeWindow(user, timeWindow).stream()
                .map(FallRiskScoreAssessment::new)
                .max(Comparator
                        .comparing((FallRiskScoreAssessment frsa) -> frsa.getFallRiskProfile()
                                .getEndTime()
                                .toLocalDate())
                        .thenComparing(FallRiskScoreAssessment::getTotal)
                )
                .map(fsa -> new LatestFallRiskProfileAssessment(user, fsa));
    }

    /**
     * Compute the latest V1FallRiskAssessment (from the latest FRP session) of a given user.
     */
    public Optional<V1FallRiskScoreAssessment> computeLatestV1FallRiskAssessmentForUser(User user) {
        Optional<FallRiskProfile> fallRiskProfileForUser = fallRiskProfileService.getLatestFallRiskProfileForUser(user);
        return fallRiskProfileForUser.map(frp -> new V1FallRiskScoreAssessment(user, frp));
    }

    /**
     * Compute a list of V1FallRiskAssessments for user within a time window.
     */
    public List<V1FallRiskScoreAssessment> computeV1FallRiskAssessmentsForUserWithinTimeWindow(
            User user,
            TimeWindow timeWindow
    ) {
        return fallRiskProfileService.getFallRiskProfilesForUserWithinTimeWindow(user, timeWindow).stream()
                .map(frp -> new V1FallRiskScoreAssessment(user, frp))
                .toList();
    }
}
