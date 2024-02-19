package smartfloor.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.fall.risk.profile.V1FallRiskScoreAssessment;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.service.FallRiskAnalysisService;
import smartfloor.service.UserService;

@Tag(name = "Fall risk analysis API", description = "Provides fall risk analyses based on footstep data.")
@RestController
@RequestMapping("/v1/analyses/fall-risk")
public class V1FallRiskAnalysisController {
    private final FallRiskAnalysisService fallRiskAnalysisService;
    private final UserService userService;

    /**
     * TODO.
     */
    @Autowired
    public V1FallRiskAnalysisController(
            FallRiskAnalysisService fallRiskAnalysisService,
            UserService userService
    ) {
        this.fallRiskAnalysisService = fallRiskAnalysisService;
        this.userService = userService;
    }

    /**
     * TODO.
     */
    @Operation(description = "Get latest fall risk assessment (with indicators) of a given user.")
    @GetMapping("/latest/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<V1FallRiskScoreAssessment> getLatestFallRiskAssessmentOfUser(@PathVariable Long userId)
            throws UserNotFoundException {
        User user = userService.getUser(userId);
        Optional<V1FallRiskScoreAssessment> latestFallRiskProfileAssessment = fallRiskAnalysisService
                .computeLatestV1FallRiskAssessmentForUser(user);
        return ResponseEntity.status(HttpStatus.OK).body(latestFallRiskProfileAssessment.orElseGet(null));
    }

    /**
     * TODO.
     */
    @Operation(description = "Get fall risk assessments (with indicators) of a given user within a time window.")
    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<V1FallRiskScoreAssessment> getFallRiskAssessmentsOfUserWithinTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return new ArrayList<>(fallRiskAnalysisService
                .computeV1FallRiskAssessmentsForUserWithinTimeWindow(user, new TimeWindow(beginTime, endTime)));
    }
}
