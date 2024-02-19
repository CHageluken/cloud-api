package smartfloor.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.repository.jpa.FootstepRepository;

/**
 * Allows for some read operations on footsteps.
 * No Create, Update & Delete operations are provided as footsteps are immutable objects that should
 * only be written by the footstep processor service.
 */
@Service
public class FootstepService {
    private final FootstepRepository footstepRepository;
    private final UserWearableLinkService userWearableLinkService;
    private final AuthorizationService authorizationService;

    /**
     * TODO.
     */
    @Autowired
    public FootstepService(
            FootstepRepository footstepRepository,
            UserWearableLinkService userWearableLinkService,
            AuthorizationService authorizationService
    ) {
        this.footstepRepository = footstepRepository;
        this.userWearableLinkService = userWearableLinkService;
        this.authorizationService = authorizationService;
    }

    void addFootstep(Footstep footstep) {
        footstepRepository.save(footstep);
    }

    /**
     * TODO.
     */
    public List<Footstep> getForWearableWithinTimeWindow(String wearableId, TimeWindow timeWindow) {
        return footstepRepository.findByWearableIdAndTimeBetweenOrderByTimeAsc(
                wearableId,
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    /**
     * TODO.
     */
    public List<Footstep> getForWearableWithinTimeWindow(Wearable wearable, TimeWindow timeWindow) {
        return footstepRepository.findByWearableIdAndTimeBetweenOrderByTimeAsc(
                wearable.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    /**
     * TODO.
     */
    int getCountByWearableWithinTimeWindow(Wearable wearable, TimeWindow timeWindow) {
        return footstepRepository.countByWearableIdAndTimeBetween(
                wearable.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    public Set<FootstepRepository.WearableStepCount> getCountsWithinTimeWindow(Date beginTime, Date endTime) {
        return footstepRepository.findWearableStepCounts(beginTime, endTime);
    }

    /**
     * TODO.
     */
    public List<Footstep> getForUserWithinTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);

        return userWearableLinkService.getByUserWithOverlappingTimeWindow(user, timeWindow).stream()
                .map(uwl -> footstepRepository.findByWearableIdAndTimeBetweenOrderByTimeAsc(
                        uwl.getWearable().getId(),
                        uwl.getBeginTime(),
                        uwl.getEndTime()
                ))
                .flatMap(Collection::stream)
                .filter(timeWindow::includes)
                .sorted()
                .toList();
    }

    /**
     * Returns the Euclidean distance between two footsteps in the Cartesian coordinate system (x, y).
     */
    static double getEuclideanDistanceBetweenFootsteps(Footstep f1, Footstep f2) {
        if (f1.getPosition() != null && f2.getPosition() != null) {
            double deltaX = Math.abs(f1.getPosition().getX() - f2.getPosition().getX());
            double deltaY = Math.abs(f1.getPosition().getY() - f2.getPosition().getY());
            return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        }
        return 0.0;
    }

}
