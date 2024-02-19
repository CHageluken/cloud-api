package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.service.AuthorizationService;
import smartfloor.service.WearableService;

@Tag(name = "Wearable API", description = "Provides various CRUD operations for handling wearables.")
@RestController
@RequestMapping("/wearables")
public class WearableController {

    private final WearableService wearableService;
    private final AuthorizationService authorizationService;

    /**
     * TODO.
     */
    @Autowired
    public WearableController(
            WearableService wearableService,
            AuthorizationService authorizationService
    ) {
        this.wearableService = wearableService;
        this.authorizationService = authorizationService;
    }

    /**
     * Retrieve a specific wearable with provided identifier.
     */
    @Operation(description = "Retrieve a wearable with provided identifier, if it is currently managed by the tenant.")
    @GetMapping("/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public Wearable getWearable(@PathVariable String wearableId) throws WearableNotFoundException {
        authorizationService.validateCurrentWearableOperationAuthority(wearableId);
        return wearableService.getWearable(wearableId);
    }

    /**
     * Retrieves a list of all wearables.
     */
    @Operation(description = "Retrieve all wearables.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Wearable> getWearables() {
        return wearableService.getWearables();
    }
}
