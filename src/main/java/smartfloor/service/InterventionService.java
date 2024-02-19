package smartfloor.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.dto.interventions.CreateInterventionsForm;
import smartfloor.domain.dto.interventions.InterventionForm;
import smartfloor.domain.dto.interventions.UpdateInterventionForm;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.interventions.FallPreventionProgram;
import smartfloor.domain.entities.interventions.Intervention;
import smartfloor.domain.entities.interventions.InterventionType;
import smartfloor.domain.exception.InterventionNotFoundException;
import smartfloor.domain.exception.InvalidInterventionsException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.repository.jpa.InterventionRepository;

/**
 * Service class which handles all business logic for intervention related operations.
 */
@Service
public class InterventionService {
    private final InterventionRepository interventionRepository;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    /**
     * Intervention service constructor.
     */
    @Autowired
    public InterventionService(
            InterventionRepository interventionRepository,
            UserService userService,
            AuthorizationService authorizationService
    ) {
        this.interventionRepository = interventionRepository;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    /**
     * Get an intervention.
     *
     * @param interventionId ID of the intervention to get.
     */
    public Intervention getInterventionById(Long interventionId) throws InterventionNotFoundException {
        return interventionRepository.findById(interventionId)
                .orElseThrow(() -> new InterventionNotFoundException(interventionId));
    }

    /**
     * Get multiple (non-deleted) user interventions in a time window.
     *
     * @param userId ID of the user.
     * @param timeWindow A combination of begin and end time.
     * @return List of interventions.
     */
    public List<Intervention> getNonDeletedInterventionsForUserBetweenTimes(
            Long userId,
            TimeWindow timeWindow
    ) {
        return interventionRepository.findByUserBetweenTimesAndDeleted(
                userId,
                false,
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    /**
     * Get all intervention types.
     *
     * @return List of intervention types.
     */
    public List<InterventionType> listInterventionTypes() {
        return Arrays.asList(InterventionType.values());
    }

    /**
     * Get all fall prevention programs.
     *
     * @return List of programs.
     */
    public List<FallPreventionProgram> listFallPreventionPrograms() {
        return Arrays.asList(FallPreventionProgram.values());
    }

    /**
     * Create multiple interventions.
     *
     * @param createInterventionsForm A DTO which provides a list of intervention forms and a user associated to all of
     * them.
     * @return List of created interventions.
     */
    public List<Intervention> createInterventions(CreateInterventionsForm createInterventionsForm)
            throws UserNotFoundException, InvalidInterventionsException, UserIsArchivedException {
        authorizationService.validateUserOperationAuthority(createInterventionsForm.getUserId());
        User user = userService.getUser(createInterventionsForm.getUserId());
        if (user.isArchived()) {
            throw new UserIsArchivedException();
        }

        List<InterventionForm> interventionForms = createInterventionsForm.getInterventions();
        if (!interventionForms.isEmpty()) {
            List<Intervention> newInterventions = interventionForms.stream().map(form ->
                    Intervention.builder()
                            .user(user)
                            .type(form.getInterventionType())
                            .beginTime(form.getBeginTime())
                            .endTime(form.getEndTime())
                            .fallPreventionProgram(form.getFallPreventionProgram())
                            .otherProgram(form.getOtherProgram())
                            .build()
            ).toList();
            newInterventions = interventionRepository.saveAll(newInterventions);
            return newInterventions;
        } else {
            throw new InvalidInterventionsException();
        }
    }

    /**
     * Update (the end time of) an intervention.
     *
     * @param interventionId ID of the intervention to update.
     * @param updateInterventionForm DTO used for the update.
     * @return An updated intervention.
     */
    public Intervention updateIntervention(Long interventionId, UpdateInterventionForm updateInterventionForm)
            throws InterventionNotFoundException, UserIsArchivedException {
        Intervention intervention = getInterventionById(interventionId);
        User user = intervention.getUser();
        authorizationService.validateUserOperationAuthority(user);
        if (user.isArchived()) {
            throw new UserIsArchivedException();
        }

        intervention.setEndTime(updateInterventionForm.getEndTime());
        return interventionRepository.save(intervention);
    }

    /**
     * Soft-delete an intervention.
     *
     * @param interventionId ID of the intervention to soft-delete.
     */
    public void softDeleteIntervention(Long interventionId)
            throws InterventionNotFoundException, UserIsArchivedException {
        Intervention intervention = getInterventionById(interventionId);
        User user = intervention.getUser();
        authorizationService.validateUserOperationAuthority(user);
        if (user.isArchived()) {
            throw new UserIsArchivedException();
        }
        interventionRepository.softDelete(intervention.getId());
    }
}
