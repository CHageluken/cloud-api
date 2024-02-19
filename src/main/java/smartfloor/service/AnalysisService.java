package smartfloor.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.AverageStrideFrequency;
import smartfloor.domain.indicators.footstep.AverageStrideLength;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.indicators.footstep.FirstToLastStepDistance;

@Service
public class AnalysisService {

    private final FootstepService footstepService;
    private final AuthorizationService authorizationService;

    /**
     * Constructor.
     *
     * @param footstepService        service for Footstep
     * @param authorizationService   service for Authorization
     */
    @Autowired
    public AnalysisService(FootstepService footstepService, AuthorizationService authorizationService) {
        this.footstepService = footstepService;
        this.authorizationService = authorizationService;
    }

    /**
     * Get covered distance for user and timewindow.
     * @param user           user
     * @param timeWindow     begin- and endtime
     * @return
     */
    public CoveredDistance getCoveredDistanceForUserWithinTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);

        List<Footstep> footsteps = footstepService.getForUserWithinTimeWindow(user, timeWindow);
        return CoveredDistance.of(footsteps);
    }

    /**
     * Get covered distance for wearable and timewindow.
     * @param wearable     wearable
     * @param timeWindow   begin- and endtime
     * @return
     */
    public CoveredDistance getCoveredDistanceForWearableWithinTimeWindow(Wearable wearable, TimeWindow timeWindow) {
        List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        return CoveredDistance.of(footsteps);
    }

    /**
     * TODO.
     */
    public FirstToLastStepDistance getFirstToLastStepDistanceForWearableWithinTimeWindow(
            Wearable wearable,
            TimeWindow timeWindow
    ) {
        List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        return new FirstToLastStepDistance(footsteps);
    }

    /**
     * TODO.
     */
    public AverageSpeed getAverageSpeedForUserWithinTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user.getId());

        List<Footstep> footsteps = footstepService.getForUserWithinTimeWindow(user, timeWindow);
        List<Footstep> footstepsWithPosition =
                footsteps.stream().filter(Footstep::hasPosition).toList();
        return AverageSpeed.of(footstepsWithPosition);
    }

    /**
     * TODO.
     */
    public AverageSpeed getAverageSpeedForWearableWithinTimeWindow(Wearable wearable, TimeWindow timeWindow) {
        List<Footstep> footsteps = footstepService.getForWearableWithinTimeWindow(wearable, timeWindow);
        List<Footstep> footstepsWithPosition =
                footsteps.stream().filter(Footstep::hasPosition).toList();
        return AverageSpeed.of(footstepsWithPosition);
    }

    /**
     * TODO.
     */
    public AverageStrideLength getAverageStrideLengthForUserWithinTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);

        List<Footstep> footsteps = footstepService.getForUserWithinTimeWindow(user, timeWindow);
        List<Footstep> footstepsWithPosition =
                footsteps.stream().filter(Footstep::hasPosition).toList();
        return AverageStrideLength.of(footstepsWithPosition);
    }

    /**
     * TODO.
     */
    public AverageStrideFrequency getStrideFrequencyForUserWithinTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);

        List<Footstep> footsteps = footstepService.getForUserWithinTimeWindow(user, timeWindow);
        List<Footstep> footstepsWithPosition =
                footsteps.stream().filter(Footstep::hasPosition).toList();
        return AverageStrideFrequency.of(footstepsWithPosition);
    }
}
