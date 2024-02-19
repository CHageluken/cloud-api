package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.repository.jpa.FootstepRepository;
import smartfloor.service.FootstepService;
import smartfloor.service.UserService;
import smartfloor.service.WearableService;

@Tag(name = "Footsteps API", description = "Provides operations for reading stored footsteps.")
@RestController
@RequestMapping("/footsteps")
public class FootstepController {

    private final FootstepService footstepService;
    private final WearableService wearableService;
    private final UserService userService;

    /**
     * TODO.
     */
    @Autowired
    public FootstepController(
            FootstepService footstepService,
            WearableService wearableService,
            UserService userService
    ) {
        this.footstepService = footstepService;
        this.wearableService = wearableService;
        this.userService = userService;
    }

    @Operation(description = "Get all footsteps for a given user within a given time window.")
    @GetMapping(value = "/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<Footstep> getForUserWithinTimeWindow(
            @PathVariable("userId") Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return footstepService.getForUserWithinTimeWindow(user, new TimeWindow(beginTime, endTime));
    }

    @Operation(description = "Get all footsteps for a given wearable within a given time window.")
    @GetMapping(value = "/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public List<Footstep> getForWearableWithinTimeWindow(
            @PathVariable("wearableId") String wearableId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return footstepService.getForWearableWithinTimeWindow(wearable, new TimeWindow(beginTime, endTime));
    }

    /**
     * Retrieve number of footsteps made per wearable within a certain timeframe.
     */
    @Operation(description = "Get number of footsteps made per wearable within a certain timeframe.")
    @GetMapping(value = "/count")
    @ResponseStatus(HttpStatus.OK)
    public Set<FootstepRepository.WearableStepCount> getCountsWithinTimeWindow(
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) {
        return footstepService.getCountsWithinTimeWindow(new Date(beginTime), new Date(endTime));
    }
}
