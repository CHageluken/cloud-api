package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.fall.risk.profile.FallRiskAssessmentModel;
import smartfloor.domain.entities.fall.risk.profile.LatestFallRiskProfileAssessment;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.service.FallRiskAnalysisService;
import smartfloor.service.GroupService;
import smartfloor.service.UserService;
import smartfloor.service.WearableService;

@Tag(name = "Fall risk analysis API", description = "Provides various fall risk analyses based on footstep data.")
@RestController
@RequestMapping("/analyses/fall-risk")
public class FallRiskAnalysisController {

    private final FallRiskAnalysisService analysisService;
    private final GroupService groupService;
    private final WearableService wearableService;
    private final UserService userService;

    /**
     * TODO.
     */
    @Autowired
    public FallRiskAnalysisController(
            FallRiskAnalysisService analysisService,
            GroupService groupService,
            WearableService wearableService,
            UserService userService
    ) {
        this.analysisService = analysisService;
        this.groupService = groupService;
        this.wearableService = wearableService;
        this.userService = userService;
    }

    /**
     * Get fall risk assessment for a given (time) window of footsteps of a single wearable device.
     *
     * @param wearableId the id of the wearable
     * @param beginTime the lower bound of the time window in which to look for footsteps for the FRP
     * @param endTime the upper bound of the time window in which to look for footsteps for the FRP
     * @return a fall risk assessment
     */
    @Operation(
            description = "Get fall risk assessment for a given (time) window of footsteps of a single " +
                    "wearable device"
    )
    @GetMapping("/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public FallRiskAssessmentModel getFallRiskAssessmentForWearableWithinTimeWindow(
            @PathVariable(value = "wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return analysisService.computeFallRiskAssessmentForWearableWithinTimeWindow(
                wearable,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * <p>Get all fall risk assessments of a user within a time frame.</p>
     * <p>Note: the fall risk assessments are from non-hidden FRPs only.</p>
     *
     * @param userId the user id
     * @param beginTime the lower bound of the time window in which to look for FRPs
     * @param endTime the upper bound of the time window in which to look for FRPs
     * @return a list of fall risk assessments  based on non-hidden FRPs
     */
    @Operation(description = "Get fall risk assessments of a user within a time window")
    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<FallRiskAssessmentModel> getFallRiskAssessmentsForUserWithinTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return analysisService.computeFallRiskAssessmentsForUserWithinTimeWindow(
                user,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * Get fall risk assessments of a user's hidden FRPs. This endpoint is useful mainly to tools that need these hidden
     * FRP based assessments for further investigation.
     *
     * @param userId the user id
     * @return a list of fall risk assessments based only on the hidden FRPs for the given user
     */
    @Operation(description = "Get fall risk assessments of a user's hidden FRPs")
    @GetMapping("/include-hidden/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<FallRiskAssessmentModel> getAllFallRiskAssessmentsForUser(@PathVariable Long userId)
            throws UserNotFoundException {
        User user = userService.getUser(userId);
        return analysisService.computeHiddenFallRiskAssessmentsForUser(user.getId());
    }

    /**
     * Get fall risk assessments of a wearable's hidden FRPs. This endpoint is useful mainly to tools that need these
     * hidden FRP based assessments for further investigation.
     *
     * @param wearableId the id of the user
     * @return a list of fall risk assessments based only on the hidden FRPs for the given user
     */
    @Operation(description = "Get fall risk assessments of a user's hidden FRPs")
    @GetMapping("/include-hidden/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public List<FallRiskAssessmentModel> getFallRiskAssessmentsForWearableWithinTimeWindow(
            @PathVariable(value = "wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return analysisService.computeHiddenFallRiskAssessmentsForWearableWithinTimeWindow(
                wearable,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * This endpoint is provided for populating the latest fall risk assessment for each user in the given group. We
     * cache the result of this endpoint for 5 minutes to avoid having to recompute for each repeated request. This
     * significantly speeds up the API response for this endpoint, achieving lower latency for the FRP dashboard. See
     * VIT-714 for more information.
     *
     * @param groupId the group id of the group for which to get the latest fall risk assessments
     * @return the latest fall risk assessment for each user in the group (as a list)
     */
    @Operation(description = "Get latest best fall risk measurements for a given group of users within a time window.")
    @GetMapping("/latest/groups/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    @Cacheable(
            value = "latestFallRiskAssessments",
            key = "{#groupId, #beginTime, T(smartfloor.controller.FallRiskAnalysisController" +
                    ".CacheKeyHelper).getLastFiveMinuteInterval(#endTime)}"
    )
    public List<LatestFallRiskProfileAssessment> getLatestBestFallRiskAssessmentsForGroupWithinTimeWindow(
            @PathVariable Long groupId, @RequestParam("begin") long beginTime, @RequestParam("end") long endTime
    ) throws GroupNotFoundException {
        Group group = groupService.getGroup(groupId);
        TimeWindow timeWindow = new TimeWindow(beginTime, endTime);
        return group.getUsers().stream()
                .map(u -> analysisService.computeLatestBestFallRiskAssessmentsForUserWithinTimeWindow(u, timeWindow))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * This helper class is used to provide a utility method for determining part of the cache key for the latest (best)
     * fall risk assessment (for a group of users) endpoint defined in this controller.
     */
    private static class CacheKeyHelper {
        /**
         * This helper is used to round down the given endTime to the nearest 5 minute interval. The helper is used in
         * the cache key of the latest fall risk assessment endpoint below. Whenever a given endTime (in the request)
         * exceeds the 5-minute interval, the cache key that we determine for a given endTime will be different. This
         * will make the current cache entry no longer hold for that endTime, effectively invalidating it and forcing
         * the endpoint to recompute the latest fall risk assessment for that endTime.
         *
         * @param endTime the end time of the time window in which to look for the latest fall risk assessments
         * @return the rounded down endTime to the nearest 5 minute interval
         */
        public static long getLastFiveMinuteInterval(long endTime) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());
            int minute = dateTime.getMinute();
            int minuteRemainder = minute % 5;
            int minuteFloor = minute - minuteRemainder;
            LocalDateTime roundedDownDateTime = dateTime.withMinute(minuteFloor).withSecond(0).withNano(0);
            return roundedDownDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }
}
