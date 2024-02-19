package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.exception.FloorInaccessibleException;
import smartfloor.domain.exception.FloorNotFoundException;
import smartfloor.service.FloorService;

@Tag(
        name = "Floor API",
        description = "Provides functionality for retrieving a list of floors as well as individual floors."
)
@RestController
@RequestMapping("/floors")
public class FloorController {

    private final FloorService floorService;

    @Autowired
    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    /**
     * Get all floors available to the authenticated user.
     *
     * @return A list of all floors available.
     */
    @Operation(description = "Retrieves (a list of) all floors.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Floor> getFloors(@AuthenticationPrincipal UserDetails authenticated) {
        return floorService.getFloors(authenticated);
    }

    /**
     * Get floor for the provided identifier if and only if it is viewable by the authenticated (requesting) user.
     *
     * @return The floor for the provided identifier
     */
    @Operation(description = "Retrieves floor for the provided identifier.")
    @GetMapping("/{floorId}")
    @ResponseStatus(HttpStatus.OK)
    public Floor getFloor(@AuthenticationPrincipal UserDetails authenticated, @PathVariable Long floorId)
            throws FloorNotFoundException, FloorInaccessibleException {
        return floorService.getFloor(authenticated, floorId);
    }
}
