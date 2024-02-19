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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.dto.UserLimit;
import smartfloor.domain.entities.Group;
import smartfloor.domain.exception.GroupAlreadyExistsException;
import smartfloor.domain.exception.GroupLimitExceededTenantLimitException;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.UserCountExceededNewLimitException;
import smartfloor.service.GroupService;

@Tag(name = "Groups API", description = "Provides CRUD operations for working with groups.")
@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    /**
     * TODO.
     */
    @Autowired
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }


    /**
     * Retrieve all groups that the authenticated user can view.
     */
    @Operation(description = "Retrieve all groups that the authenticated user can view.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Group> getGroups() {
        return groupService.getGroups();
    }

    /**
     * Retrieve the group with the provided identifier.
     */
    @Operation(description = "Retrieve a group by its identifier.")
    @GetMapping("/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public Group getGroupById(@PathVariable Long groupId) throws GroupNotFoundException {
        return groupService.getGroup(groupId);
    }

    /**
     * Retrieve user limit for group with provided identifier.
     */
    @Operation(description = "Get user limit of group with given identifier.")
    @GetMapping("/{groupId}/user-limit")
    @ResponseStatus(HttpStatus.OK)
    public UserLimit getUserLimit(@PathVariable Long groupId) throws GroupNotFoundException {
        Group group = groupService.getGroup(groupId);
        return new UserLimit(group.getUserLimit());
    }

    /**
     * Set user limit for group with provided identifier.
     */
    @Operation(description = "Set user limit of group with given identifier.")
    @PutMapping("/{groupId}/user-limit")
    @ResponseStatus(HttpStatus.OK)
    public UserLimit setUserLimit(
            @AuthenticationPrincipal UserDetails authenticated,
            @PathVariable Long groupId,
            @RequestBody UserLimit groupUserLimit
    ) throws
            GroupAlreadyExistsException,
            GroupLimitExceededTenantLimitException,
            UserCountExceededNewLimitException,
            GroupNotFoundException {
        return groupService.updateGroupUserLimit(authenticated, groupId, groupUserLimit);
    }
}
