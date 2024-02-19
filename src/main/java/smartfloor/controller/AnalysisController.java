package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.AverageStrideFrequency;
import smartfloor.domain.indicators.footstep.AverageStrideLength;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.indicators.footstep.FirstToLastStepDistance;
import smartfloor.service.AnalysisService;
import smartfloor.service.UserService;
import smartfloor.service.WearableService;

@Tag(name = "Base analysis API", description = "Provides various footstep parameter analyses based on footstep data.")
@RestController
@RequestMapping("/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final UserService userService;
    private final WearableService wearableService;

    /**
     * TODO.
     */
    @Autowired
    public AnalysisController(
            AnalysisService analysisService,
            UserService userService,
            WearableService wearableService
    ) {
        this.analysisService = analysisService;
        this.userService = userService;
        this.wearableService = wearableService;
    }

    /*
     ****************************************
     * DISTANCE
     ****************************************
     */

    /**
     * Get the covered (default) distance (in mm) for a user within a time window. "Covered" distance is defined as the
     * sum of distances between consecutive footsteps within the specified time window.
     *
     * @return the covered distance for the user in mm
     */
    @Operation(
            description = "Get the distance (mm) travelled by a user between the two timestamps defined by the " +
                    "time window."
    )
    @GetMapping("/distance/cumulative/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public CoveredDistance getCoveredDistanceForUserWithinTimeWindow(
            @PathVariable(value = "userId") Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return analysisService.getCoveredDistanceForUserWithinTimeWindow(user, new TimeWindow(beginTime, endTime));
    }

    /**
     * Get the covered (default) distance (in mm) for a wearable within a time window. "Covered" distance is defined as
     * the sum of distances between consecutive footsteps within the given time window.
     *
     * @return the covered distance for the wearable in mm
     */
    @Operation(description = "Get the distance (mm) travelled by a wearable in a time frame")
    @GetMapping("/distance/cumulative/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public CoveredDistance getCoveredDistanceForWearableWithinTimeWindow(
            @PathVariable(value = "wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return analysisService.getCoveredDistanceForWearableWithinTimeWindow(
                wearable,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * Get the first to last step distance (in mm) for a wearable within a time window. First to last step distance is
     * defined as the distance between the first stride with a position to the last stride with a position within the
     * given time window.
     *
     * @return the first-to-last step distance for the wearable in mm
     */
    @Operation(description = "Get the distance (mm) travelled by a wearable in a time frame")
    @GetMapping("/distance/first-to-last/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public FirstToLastStepDistance getFirstToLastStepDistanceForWearableWithinTimeWindow(
            @PathVariable(value = "wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return analysisService.getFirstToLastStepDistanceForWearableWithinTimeWindow(
                wearable,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * Get the average speed (in mm/s) for a user within a time window.
     *
     * @return the average walking speed for the user in mm/s
     */
    @Operation(description = "Get the average speed (mm/s) of a user in a time frame")
    @GetMapping("/speed/average/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public AverageSpeed getAverageSpeedForUserWithinTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return analysisService.getAverageSpeedForUserWithinTimeWindow(user, new TimeWindow(beginTime, endTime));
    }

    /**
     * Get the average speed (in mm/s) for a wearable within a time window.
     *
     * @return the average walking speed for the wearable in mm/s
     */
    @Operation(description = "Get the average speed (mm/s) of a wearable in a time frame")
    @GetMapping("/speed/average/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public AverageSpeed getAverageSpeedForWearableWithinTimeWindow(
            @PathVariable String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return analysisService.getAverageSpeedForWearableWithinTimeWindow(wearable, new TimeWindow(beginTime, endTime));
    }

    /**
     * Get the average footstep (or stride) length (in mm) for a user within a time window.
     *
     * @return the average footstep (or stride) length in mm
     */
    @Operation(description = "Get the average footstep length (mm) of a user in a time frame")
    @GetMapping("/stride-length/average/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public AverageStrideLength getAverageStrideLengthForUserWithinTimeWindow(
            @PathVariable(value = "userId") Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return analysisService.getAverageStrideLengthForUserWithinTimeWindow(user, new TimeWindow(beginTime, endTime));
    }

    /**
     * Get the average step (or stride) frequency (in strides/second) for a user within a time window.
     *
     * @return the average step (or stride) frequency in strides/second
     */
    @Operation(description = "Get the average footstep frequency (step/s) of a user in a time frame")
    @GetMapping("/stride-frequency/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public AverageStrideFrequency getAverageStrideFrequencyForUserWithinTimeWindow(
            @PathVariable(value = "userId") Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return analysisService.getStrideFrequencyForUserWithinTimeWindow(user, new TimeWindow(beginTime, endTime));
    }
}
