package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.dto.UnlinkActiveUserForm;
import smartfloor.domain.dto.UnlinkActiveWearableForm;
import smartfloor.domain.dto.UserWearableLinkForm;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.UserNotLinkedException;
import smartfloor.domain.exception.WearableInUseException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.domain.exception.WearableNotInUseException;
import smartfloor.service.AuthorizationService;
import smartfloor.service.GroupService;
import smartfloor.service.UserService;
import smartfloor.service.UserWearableLinkService;
import smartfloor.service.WearableService;

@Tag(name = "User wearable link API", description = "Provides various CRUD operations for user wearable links.")
@RestController
@RequestMapping("/user-wearable-links")
public class UserWearableLinkController {
    private final UserWearableLinkService userWearableLinkService;
    private final UserService userService;
    private final WearableService wearableService;
    private final GroupService groupService;
    private final AuthorizationService authorizationService;

    /**
     * TODO.
     */
    @Autowired
    public UserWearableLinkController(
            UserWearableLinkService userWearableLinkService,
            UserService userService,
            WearableService wearableService,
            GroupService groupService,
            AuthorizationService authorizationService
    ) {
        this.userWearableLinkService = userWearableLinkService;
        this.userService = userService;
        this.wearableService = wearableService;
        this.groupService = groupService;
        this.authorizationService = authorizationService;
    }

    /**
     * Retrieve all user wearable links for a single user corresponding with provided identifier.
     */
    @Operation(description = "Retrieve all user wearable link for the specified userId.")
    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<UserWearableLink> getUserWearableLinksForUser(@PathVariable Long userId) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return userWearableLinkService.getByUserId(user.getId());
    }

    /**
     * Retrieve all user wearable links for a single wearable corresponding with provided identifier.
     */
    @Operation(description = "Retrieve all user wearable link for the specified wearableId.")
    @GetMapping("/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public List<UserWearableLink> getUserWearableLinksForWearable(@PathVariable String wearableId)
            throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return userWearableLinkService.getByWearableId(wearable.getId());
    }

    /**
     * Retrieve all user wearable links for a given time window.
     */
    @Operation(description = "Retrieve all user wearable links for the specified time frame.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserWearableLink> getUserWearableLinksForTimeWindow(
            @RequestParam("begin") long beginTime,
            @RequestParam("end") long endTime
    ) {
        return userWearableLinkService.getByOverlappingTimeWindow(new TimeWindow(beginTime, endTime));
    }

    /**
     * <p>Create a new user-wearable link for a given user and wearable combination through the provided DTO object.</p>
     * #446: The user-wearable link form's begin and end time will not be used for determining the
     * begin and end time of the user-wearable link. Instead, the link will have its begin time set to the current time
     * and its end time undetermined (null).
     *
     * @param userWearableLinkForm the DTO object that describes the user-wearable link to be created
     * @return the user-wearable link that was created
     * @throws UserNotFoundException     if the user in the to be created user-wearable link does not exist
     * @throws WearableNotFoundException if the wearable in the to be created user-wearable link does not exist
     * @throws WearableInUseException    if the wearable is currently in use (already actively linked)
     */
    @Operation(description = "Create a user-wearable link for a given user and wearable combination.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserWearableLink link(@RequestBody UserWearableLinkForm userWearableLinkForm)
            throws UserNotFoundException, WearableNotFoundException, WearableInUseException, UserIsArchivedException {
        User user = userService.getUser(userWearableLinkForm.getUserId());
        authorizationService.validateCurrentWearableOperationAuthority(userWearableLinkForm.getWearableId());
        Wearable wearable = wearableService.getWearable(userWearableLinkForm.getWearableId());
        UserWearableLink userWearableLink = new UserWearableLink(userWearableLinkForm, user, wearable);
        return userWearableLinkService.createUserWearableLink(userWearableLink);
    }

    /**
     * Complete an active user-wearable link for a given wearable through the provided DTO object.
     * This is equivalent to "unlinking" a currently linked user from the given wearable.
     *
     * @param unlinkActiveWearableForm the DTO object that describes the wearable of which the active link is to be
     *                                 completed
     * @return the user-wearable link that was completed
     * @throws WearableNotFoundException if the wearable in the to be completed user-wearable link does not exist
     */
    @Operation(description = "Complete an active user-wearable link for a given wearable (effectively unlinking it).")
    @PostMapping("/wearables/unlink")
    @ResponseStatus(HttpStatus.OK)
    public UserWearableLink unlink(@RequestBody UnlinkActiveWearableForm unlinkActiveWearableForm)
            throws WearableNotFoundException, WearableNotInUseException {
        authorizationService.validateCurrentWearableOperationAuthority(unlinkActiveWearableForm.getWearableId());
        Wearable wearable = wearableService.getWearable(unlinkActiveWearableForm.getWearableId());
        UserWearableLink active = userWearableLinkService.getActiveByWearable(wearable)
                .orElseThrow(() -> new WearableNotInUseException(wearable.getId()));
        active.complete();
        return userWearableLinkService.updateUserWearableLink(active);
    }

    /**
     * Complete an active user-wearable link for a given user through the provided DTO object.
     * This is equivalent to "unlinking" a currently linked wearable from the given user.
     *
     * @param unlinkActiveUserForm the DTO object that describes the user of which the active link is to be completed
     * @return the user-wearable link that was completed
     * @throws UserNotFoundException if the user in the to be completed user-wearable link does not exist
     */
    @Operation(description = "Complete an active user-wearable link for a given wearable (effectively unlinking it).")
    @PostMapping("/users/unlink")
    @ResponseStatus(HttpStatus.OK)
    public UserWearableLink unlink(@RequestBody UnlinkActiveUserForm unlinkActiveUserForm)
            throws UserNotFoundException, UserNotLinkedException {
        User user = userService.getUser(unlinkActiveUserForm.getUserId());
        UserWearableLink active = userWearableLinkService.getActiveByUser(user)
                .orElseThrow(() -> new UserNotLinkedException(user.getId()));
        active.complete();
        return userWearableLinkService.updateUserWearableLink(active);
    }

    /**
     * TODO.
     */
    @Operation(description = "Get active user wearable links for a group of users.")
    @GetMapping("/active/groups/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public List<UserWearableLink> getActiveWearableLinksForGroup(@PathVariable Long groupId)
            throws GroupNotFoundException {
        Group group = groupService.getGroup(groupId);
        return userWearableLinkService.getActiveWearableLinksForGroup(group);
    }

    /**
     * TODO.
     */
    @Operation(description = "Get active user wearable link for a user.")
    @GetMapping("/active/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserWearableLink> getActiveWearableLinkForUser(@PathVariable Long userId)
            throws UserNotFoundException {
        User user = userService.getUser(userId);
        Optional<UserWearableLink> uwl = userWearableLinkService.getActiveByUser(user);
        return uwl.map(userWearableLink -> ResponseEntity.ok().body(userWearableLink))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * TODO.
     */
    @Operation(description = "Get active user wearable link for a wearable.")
    @GetMapping("/active/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserWearableLink> getActiveWearableLinkForWearable(@PathVariable String wearableId)
            throws WearableNotFoundException {
        authorizationService.validateCurrentWearableOperationAuthority(wearableId);
        Wearable wearable = wearableService.getWearable(wearableId);
        Optional<UserWearableLink> uwl = userWearableLinkService.getActiveByWearable(wearable);
        return uwl.map(userWearableLink -> ResponseEntity.ok().body(userWearableLink))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(description = "Check whether a wearable has an active link.")
    @GetMapping("/is-active/wearables/{wearableId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Boolean> isWearableActive(@PathVariable String wearableId) throws WearableNotFoundException {
        Wearable wearable = wearableService.getWearable(wearableId);
        return ResponseEntity.ok().body(userWearableLinkService.isWearableActive(wearable));
    }
}
