package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.dto.interventions.CreateInterventionsForm;
import smartfloor.domain.dto.interventions.UpdateInterventionForm;
import smartfloor.domain.entities.interventions.FallPreventionProgram;
import smartfloor.domain.entities.interventions.Intervention;
import smartfloor.domain.entities.interventions.InterventionType;
import smartfloor.domain.exception.InterventionNotFoundException;
import smartfloor.domain.exception.InvalidInterventionsException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.service.InterventionService;

@Tag(name = "Interventions API", description = "Provides CRUD operations for interventions and intervention types.")
@RestController
@RequestMapping("/interventions")
public class InterventionController {
    private final InterventionService interventionService;

    /**
     * Provides CRUD operations for interventions and intervention types.
     */
    @Autowired
    public InterventionController(InterventionService interventionService) {
        this.interventionService = interventionService;
    }

    /**
     * Create a list of interventions.
     *
     * @param createInterventionsForm DTO which specifies the user the interventions belong to, and the interventions
     * themselves.
     * @return A list of the created interventions.
     */
    @Operation(description = "Create multiple interventions.")
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public List<Intervention> createInterventions(@RequestBody CreateInterventionsForm createInterventionsForm)
            throws UserNotFoundException, InvalidInterventionsException, UserIsArchivedException {
        return interventionService.createInterventions(createInterventionsForm);
    }

    /**
     * Soft-delete an intervention.
     *
     * @param interventionId ID of the intervention to delete.
     */
    @Operation(description = "Soft delete an intervention with provided identifier.")
    @DeleteMapping("/{interventionId}")
    @ResponseStatus(HttpStatus.OK)
    public void softDeleteIntervention(
            @PathVariable(value = "interventionId") Long interventionId
    ) throws InterventionNotFoundException, UserIsArchivedException {
        interventionService.softDeleteIntervention(interventionId);
    }

    /**
     * Update an intervention.
     *
     * @param updateInterventionForm A form that defined the fields to update. For now, this is just the end date.
     * @param interventionId ID of the intervention to update.
     * @return An updated intervention.
     */
    @Operation(description = "Update intervention with provided identifier and data.")
    @PutMapping("/{interventionId}")
    @ResponseStatus(HttpStatus.OK)
    public Intervention updateIntervention(
            @RequestBody UpdateInterventionForm updateInterventionForm,
            @PathVariable Long interventionId
    )
            throws InterventionNotFoundException, UserIsArchivedException {
        return interventionService.updateIntervention(interventionId, updateInterventionForm);
    }

    /**
     * Retrieve interventions for a user in a time window.
     *
     * @param userId ID of the user the interventions belong to.
     * @param beginTime Of the time window.
     * @param endTime Of the time window.
     * @return A list of interventions.
     */
    @Operation(description = "Retrieve interventions for a user in a time window.")
    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<Intervention> getInterventionsForUserBetweenTimes(
            @PathVariable(value = "userId") Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) {
        return interventionService.getNonDeletedInterventionsForUserBetweenTimes(
                userId,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * Get intervention types.
     *
     * @return List of types.
     */
    @Operation(description = "Retrieve intervention types.")
    @GetMapping("/types")
    @ResponseStatus(HttpStatus.OK)
    public List<InterventionType> getInterventionTypes() {
        return interventionService.listInterventionTypes();
    }

    /**
     * Get fall prevention programs.
     *
     * @return List of programs.
     */
    @Operation(description = "Retrieve fall prevention programs.")
    @GetMapping("/fall-prevention-programs")
    @ResponseStatus(HttpStatus.OK)
    public List<FallPreventionProgram> getFallPreventionPrograms() {
        return interventionService.listFallPreventionPrograms();
    }
}
