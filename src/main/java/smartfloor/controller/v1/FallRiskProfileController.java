package smartfloor.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.dto.FallRiskProfileNoteForm;
import smartfloor.domain.dto.FallRiskProfileRemovalForm;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemovalReason;
import smartfloor.domain.exception.FallRiskProfileNotFoundException;
import smartfloor.service.FallRiskProfileService;

@Tag(
        name = "V1 Fall risk profile API", description = "Exposes endpoints related to FRP soft-deletion. All other " +
        "FRP related actions are handled by lambdas."
)
@RestController
@RequestMapping("/v1/fall-risk-profiles")
public class FallRiskProfileController {
    private final FallRiskProfileService fallRiskProfileService;

    /**
     * Defines soft-deletion operations for fall risk profiles.
     */
    @Autowired
    public FallRiskProfileController(
            FallRiskProfileService fallRiskProfileService
    ) {
        this.fallRiskProfileService = fallRiskProfileService;
    }

    /**
     * Soft-delete an FRP with a provided identifier. Allows for adding details regarding the deletion.
     *
     * @param frpId FRP to be deleted.
     * @param user The (authenticated) user, who deletes the FRP.
     * @param fallRiskProfileRemovalForm A form for providing deletion details.
     * @throws FallRiskProfileNotFoundException When an FRP with the provided ID does not exist.
     */
    @Operation(description = "Soft-delete a fall risk profile.")
    @DeleteMapping("/{frpId}")
    @ResponseStatus(HttpStatus.OK)
    public void softDeleteFRP(
            @PathVariable Long frpId,
            @AuthenticationPrincipal User user,
            @RequestBody FallRiskProfileRemovalForm fallRiskProfileRemovalForm
    ) throws FallRiskProfileNotFoundException {
        fallRiskProfileService.softDeleteFallRiskProfile(frpId, user, fallRiskProfileRemovalForm);
    }

    @Operation(description = "Create, update or delete a fall risk profile note.")
    @PutMapping("/{frpId}")
    @ResponseStatus(HttpStatus.OK)
    public FallRiskProfile manageFRPNote(
            @PathVariable Long frpId,
            @AuthenticationPrincipal User user,
            @RequestBody FallRiskProfileNoteForm fallRiskProfileNoteForm
    ) throws FallRiskProfileNotFoundException {
        return fallRiskProfileService.manageFRPNote(frpId, user, fallRiskProfileNoteForm);
    }

    @Operation(description = "Get list of possible reasons for FRP soft-deletion.")
    @GetMapping("/removal-reasons")
    @ResponseStatus(HttpStatus.OK)
    public List<FallRiskProfileRemovalReason> getRemovalReasons() {
        return fallRiskProfileService.getRemovalReasons();
    }
}
