package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.UserMeasurementType;
import smartfloor.domain.dto.CreateWearableUserForm;
import smartfloor.domain.dto.UpdateWearableUserForm;
import smartfloor.domain.dto.user.measurements.UserMeasurementForm;
import smartfloor.domain.dto.user.measurements.validators.UserMeasurementDetailsValidator;
import smartfloor.domain.dto.user.measurements.validators.UserMeasurementFormDetailsValidator;
import smartfloor.domain.entities.Application;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserMeasurement;
import smartfloor.domain.exception.CannotUnarchiveUserException;
import smartfloor.domain.exception.CompositeUserNotFoundException;
import smartfloor.domain.exception.GroupAlreadyExistsException;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.GroupUserLimitReachedException;
import smartfloor.domain.exception.InvalidMeasurementTypeException;
import smartfloor.domain.exception.TenantNotFoundException;
import smartfloor.domain.exception.TenantUserLimitReachedException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserMeasurementNotFoundException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.repository.jpa.UserMeasurementRepository;
import smartfloor.service.CompositeUserService;
import smartfloor.service.GroupService;
import smartfloor.service.UserService;

@Tag(name = "User API", description = "Provides various CRUD operations for handling users.")
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final GroupService groupService;

    private final CompositeUserService compositeUserService;
    private final UserMeasurementRepository userMeasurementRepository;

    private final UserMeasurementDetailsValidator userMeasurementDetailsValidator;

    /**
     * TODO.
     */
    @Autowired
    public UserController(
            UserService userService,
            GroupService groupService,
            CompositeUserService compositeUserService,
            UserMeasurementRepository userMeasurementRepository,
            UserMeasurementDetailsValidator userMeasurementDetailsValidator
    ) {
        this.userService = userService;
        this.groupService = groupService;
        this.compositeUserService = compositeUserService;
        this.userMeasurementRepository = userMeasurementRepository;
        this.userMeasurementDetailsValidator = userMeasurementDetailsValidator;
    }

    @InitBinder("userMeasurementForm")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(new UserMeasurementFormDetailsValidator(userMeasurementDetailsValidator));
    }

    /**
     * Create a wearable user based on the provided CreateWearableUserForm DTO and assign it to a group.
     */
    @Operation(description = "Create a new wearable user and optionally assign it to a user group.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public User createWearableUser(
            @RequestBody CreateWearableUserForm createWearableUserForm,
            @AuthenticationPrincipal User authenticatedUser
    ) throws
            TenantNotFoundException,
            TenantUserLimitReachedException,
            GroupUserLimitReachedException,
            GroupAlreadyExistsException,
            GroupNotFoundException {
        Group group = groupService.getGroupForUpdate(createWearableUserForm.getUserGroupId());
        User newUser =
                userService.createWearableUserWithInfo(group, authenticatedUser, createWearableUserForm.getInfo());
        group.getUsers(true).add(newUser);
        groupService.updateGroup(group);
        return newUser;
    }

    /**
     * Retrieve all users.
     */
    @Operation(description = "Retrieve all users.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<User> getUsers() {
        return userService.getUsers();
    }

    /**
     * Retrieve single user corresponding with provided identifier.
     */
    @Operation(description = "Retrieve user for the provided identifier.")
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public User getUser(@PathVariable Long userId) throws UserNotFoundException {
        return userService.getUser(userId);
    }

    /**
     * Retrieve authenticated User or CompositeUser.
     */
    @Operation(description = "Retrieve currently authenticated object (User or CompositeUser).")
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserDetails getAuthenticatedObject(@AuthenticationPrincipal UserDetails userDetails) {
        return userDetails;
    }

    /**
     * Delete a user account.
     */
    @Operation(description = "Delete user account with provided identifier.")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteUser(@PathVariable Long userId) throws UserNotFoundException {
        log.info("Attempting to delete user with id " +  userId);
        userService.deleteUser(userId);
    }

    /**
     * TODO.
     */
    @Operation(description = "Update user account with provided identifier.")
    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public User updateUser(@PathVariable Long userId, @RequestBody UpdateWearableUserForm updateWearableUserForm)
            throws UserNotFoundException, UserIsArchivedException, CompositeUserNotFoundException {
        User user = userService.getUser(userId);

        if (updateWearableUserForm.getInfo() != null &&
                (user.getInfo() == null || !user.getInfo().equals(updateWearableUserForm.getInfo()))) {
            user.setInfo(updateWearableUserForm.getInfo());
        }

        Long currentCompositeUserId = user.getCompositeUser() != null ? user.getCompositeUser().getId() : null;
        if (!Objects.equals(updateWearableUserForm.getCompositeUserId(), currentCompositeUserId)) {
            Long compositeUserIdFromForm = updateWearableUserForm.getCompositeUserId();
            if (compositeUserIdFromForm != null) {
                CompositeUser compositeUser = compositeUserService.getCompositeUser(compositeUserIdFromForm);
                user.setCompositeUser(compositeUser);
            } else {
                user.setCompositeUser(null);
            }
        }

        return userService.updateUser(user);
    }

    @Operation(description = "Retrieve user/tenant application access.")
    @GetMapping("/accessible-applications")
    @ResponseStatus(HttpStatus.OK)
    public List<Application> getAccessibleApplications(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getAccessibleApplicationsForUserDetails(userDetails);
    }

    @GetMapping("/{userId}/measurements/{measurementId}")
    @ResponseStatus(HttpStatus.OK)
    public UserMeasurement getMeasurement(@PathVariable Long userId, @PathVariable Long measurementId)
            throws UserMeasurementNotFoundException {
        return userService.getNonDeletedMeasurementById(measurementId);
    }

    @GetMapping("/{userId}/measurements")
    @ResponseStatus(HttpStatus.OK)
    public List<UserMeasurement> getMeasurements(@PathVariable Long userId) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return userService.getNonDeletedMeasurements(user);
    }

    /**
     * TODO.
     */
    @GetMapping("/{userId}/measurements/poma")
    @ResponseStatus(HttpStatus.OK)
    public List<UserMeasurement> getPOMAMeasurementsForUserAndTypeInTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return userService.getNonDeletedMeasurementsOfUserAndTypeWithinTimeWindow(
                user,
                UserMeasurementType.POMA,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * TODO.
     */
    @GetMapping("/{userId}/measurements/fall-incidents")
    @ResponseStatus(HttpStatus.OK)
    public List<UserMeasurement> getFallIncidentsForUserAndTypeInTimeWindow(
            @PathVariable Long userId,
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return userService.getNonDeletedMeasurementsOfUserAndTypeWithinTimeWindow(
                user,
                UserMeasurementType.FALL_INCIDENT,
                new TimeWindow(beginTime, endTime)
        );
    }

    /**
     * TODO.
     */
    @PostMapping("/{userId}/measurements")
    @ResponseStatus(HttpStatus.CREATED)
    public UserMeasurement recordMeasurement(
            @AuthenticationPrincipal UserDetails authenticated,
            @PathVariable Long userId,
            @Valid @RequestBody UserMeasurementForm userMeasurementForm
    ) throws UserNotFoundException, InvalidMeasurementTypeException, UserIsArchivedException {
        User user = userService.getUser(userId);
        try {
            UserMeasurement userMeasurement = UserMeasurement.fromDetails(userMeasurementForm);
            userMeasurement.setUser(user);
            if (authenticated instanceof User) {
                userMeasurement.setRecordedBy((User) authenticated);
            } else { // Authenticated as a CU
                userMeasurement.setRecordedBy(user);
            }
            return userService.recordMeasurement(userMeasurement);
        } catch (IllegalArgumentException e) {
            throw new InvalidMeasurementTypeException();
        }
    }

    /**
     * TODO.
     */
    @Operation(description = "(SOFT) Delete measurement with provided identifier.")
    @DeleteMapping("/measurements/{measurementId}")
    @ResponseStatus(HttpStatus.OK)
    public void softDeleteMeasurement(@PathVariable Long measurementId)
            throws UserMeasurementNotFoundException, UserIsArchivedException {
        UserMeasurement userMeasurement = userMeasurementRepository.findById(measurementId)
                .orElseThrow(() -> new UserMeasurementNotFoundException(measurementId));
        userService.softDeleteMeasurement(userMeasurement.getId());
    }

    @Operation(description = "Archive user with provided identifier.")
    @PatchMapping("/{userId}/archive")
    @ResponseStatus(HttpStatus.OK)
    public User archiveUser(@PathVariable Long userId) throws UserNotFoundException {
        return userService.archiveUser(userId);
    }

    @Operation(description = "Unarchive user with provided identifier.")
    @PatchMapping("/{userId}/unarchive")
    @ResponseStatus(HttpStatus.OK)
    public User unarchiveUser(@PathVariable Long userId) throws
            UserNotFoundException,
            CannotUnarchiveUserException,
            TenantUserLimitReachedException,
            GroupUserLimitReachedException {
        return userService.unarchiveUser(userId);
    }
}
