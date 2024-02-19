package smartfloor.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import smartfloor.domain.Role;
import smartfloor.domain.dto.UserLimit;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.exception.GroupAlreadyExistsException;
import smartfloor.domain.exception.GroupLimitExceededTenantLimitException;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.UserCountExceededNewLimitException;
import smartfloor.repository.jpa.GroupRepository;

/**
 * Service class which handles all business logic for group related operations.
 */
@Service
public class GroupService {
    private static final Logger log = LoggerFactory.getLogger(GroupService.class);
    private final GroupRepository groupRepository;
    private final AuthorizationService authorizationService;

    @Autowired
    public GroupService(GroupRepository groupRepository, AuthorizationService authorizationService) {
        this.groupRepository = groupRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * Create a new group using the given group object and persist it to the database.
     * We check if the name of the given group is not already used by another existing group.
     *
     * @param group to create
     * @return the created group
     */
    public Group createGroup(Group group) throws GroupAlreadyExistsException {
        boolean thereExistsAGroupWithTheSameNameForThisTenant =
                groupRepository.existsByTenantAndName(group.getTenant(), group.getName());
        if (thereExistsAGroupWithTheSameNameForThisTenant) {
            throw new GroupAlreadyExistsException(group.getName());
        }
        Group createdGroup = groupRepository.save(group);
        log.debug(
                "Created new group {} with id {} (tenant: {})",
                group.getName(),
                group.getId(),
                group.getTenant().getName()
        );
        return createdGroup;
    }

    /**
     * Update an existing group using the given group object (which should contain a valid existing group id).
     * We check if the name that we update the group is not already used by another existing group.
     *
     * @param group to update
     * @return the updated group
     */
    public Group updateGroup(Group group) throws GroupNotFoundException, GroupAlreadyExistsException {
        authorizationService.validateGroupOperationAuthority(group);

        /* Should an existing group (that does not have the same group id as the one we want to update) have the same
        name, we throw an exception stating that the group already exists. */
        boolean groupToUpdateExists = groupRepository.existsById(group.getId());
        if (!groupToUpdateExists) throw new GroupNotFoundException(group.getId());
        boolean thereExistsAGroupWithTheSameNameForThisTenant =
                groupRepository.existsByIdNotAndTenantAndName(group.getId(), group.getTenant(), group.getName());
        if (thereExistsAGroupWithTheSameNameForThisTenant) throw new GroupAlreadyExistsException(group.getName());
        log.debug(
                "Updated group {} with id {} (tenant: {})",
                group.getName(),
                group.getId(),
                group.getTenant().getName()
        );
        return groupRepository.save(group);
    }

    /**
     * Retrieve all existing groups with the constraint that only the groups are returned that are either managed by
     * or participated in by the authenticated user.
     *
     * @return a list of all existing groups managed by or participated in by the given authenticated user
     */
    public List<Group> getGroups() {
        UserDetails authenticatedUser = authorizationService.getAuthenticatedUser()
                .orElseThrow(() ->
                        new AccessDeniedException("Failed to validate (authenticated) user authorizations."));
        Collection<? extends GrantedAuthority> authorities = authenticatedUser.getAuthorities();
        boolean isAdmin = authorities.contains(Role.ADMIN.toGrantedAuthority());
        boolean isManager = authorities.contains(Role.MANAGER.toGrantedAuthority());
        boolean isComposite = authenticatedUser instanceof CompositeUser;

        if (isAdmin || isComposite) {
            return groupRepository.findAll();
        } else if (isManager) {
            return groupRepository.findGroupsByManager((User) authenticatedUser);
        } else {
            // This case will become obsolete once we migrate all regular users to composite.
            return groupRepository.findGroupsByUser((User) authenticatedUser);
        }
    }

    /**
     * Retrieve group with the provided identifier.
     *
     * @param id the identifier of the group we want to lookup
     * @return the group belonging to the given group id
     */
    public Group getGroup(Long id) throws GroupNotFoundException {
        authorizationService.validateGroupOperationAuthority(id);

        return groupRepository.findById(id).orElseThrow(() -> new GroupNotFoundException(id));
    }

    /**
     * Retrieve group with the provided name.
     *
     * @param name the name of the group we want to lookup
     * @return the group belonging to the given name
     */
    public Group getByName(String name) throws GroupNotFoundException {
        return groupRepository.findByName(name).orElseThrow(() -> new GroupNotFoundException(name));
    }

    /**
     * Delete the group belonging to the provided identifier.
     *
     * @param id the identifier of the group to be deleted
     */
    public void deleteById(Long id) throws GroupNotFoundException {
        Group group = groupRepository.findById(id).orElseThrow(() -> new GroupNotFoundException(id));
        log.debug(
                "Deleted group {} with id {} (tenant: {})",
                group.getName(),
                group.getId(),
                group.getTenant().getName()
        );
        groupRepository.delete(group);
    }

    /**
     * Get list of groups for user with provided identifier.
     */
    public List<Group> getGroupsForUserId(Long userId) {
        return groupRepository.findGroupsForUserId(userId);
    }

    /**
     * Set user limit for group with provided identifier.
     * The new group limit must not exceed:
     * The current count of its (non-archived) users;
     * The difference between the tenant limit and the limits of the remaining tenant groups;
     * The difference between the tenant limit and the user count of the remaining tenant groups.
     */
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public UserLimit updateGroupUserLimit(UserDetails authenticated, Long groupId, UserLimit groupUserLimit) throws
            GroupNotFoundException,
            UserCountExceededNewLimitException,
            GroupAlreadyExistsException,
            GroupLimitExceededTenantLimitException {
        User authenticatedUser = (User) authenticated;
        boolean groupUserLimitCanBeSet = false;
        List<Group> userGroups = getGroups();
        Integer sumOfGroupLimits = userGroups
                .stream()
                .filter(group -> !Objects.equals(group.getId(), groupId))
                .map(Group::getUserLimit)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        Integer sumOfUsersPerGroup = userGroups
                .stream()
                .filter(group -> !Objects.equals(group.getId(), groupId))
                .map(Group::getUsers)
                .map(List::size)
                .mapToInt(Integer::intValue).sum();
        Integer tenantLimit = authenticatedUser.getTenant().getUserLimit();
        boolean tenantHasUserLimit = tenantLimit != null;
        // Difference between tenant limit and sum of all group limits
        Integer highestLimitBasedOnGroupLimits = Objects.requireNonNullElse(tenantLimit, 0) - sumOfGroupLimits;
        // Difference between tenant limit and user count of all groups
        Integer highestLimitBasedOnUserCount = Objects.requireNonNullElse(tenantLimit, 0) - sumOfUsersPerGroup;
        // The smaller of the two differences is used for comparison with the limit we are trying to set
        Integer smallerHighestLimit = Math.min(highestLimitBasedOnGroupLimits, highestLimitBasedOnUserCount);
        Group group = getGroup(groupId);
        boolean userCountOfCurrentGroupDoesNotExceedNewLimit =
                groupUserLimit.getValue() == null || groupUserLimit.getValue() >= group.getUsers().size();
        boolean groupLimitIsWithinTenantLimit =
                groupUserLimit.getValue() == null || groupUserLimit.getValue() <= smallerHighestLimit;
        if (userCountOfCurrentGroupDoesNotExceedNewLimit) {
            if (tenantHasUserLimit) {
                if (groupLimitIsWithinTenantLimit) {
                    groupUserLimitCanBeSet = true;
                }
            } else {
                groupUserLimitCanBeSet = true;
            }
        } else {
            throw new UserCountExceededNewLimitException();
        }
        if (groupUserLimitCanBeSet) {
            group.setUserLimit(groupUserLimit.getValue());
            group = updateGroup(group);
            return new UserLimit(group.getUserLimit());
        } else {
            throw new GroupLimitExceededTenantLimitException(smallerHighestLimit);
        }
    }

    /**
     * Retrieve group with the provided identifier for update purposes.
     * The method delegates to the repository method that acquires a pessimistic write lock.
     * This pessimistic write lock is a row-level lock that is released when the transaction is committed.
     * Example use case: adding a user to a group after creating them.
     *
     * @param id the identifier of the group we want to lookup
     * @return the group that one wants to update
     */
    public Group getGroupForUpdate(Long id) throws GroupNotFoundException {
        authorizationService.validateGroupOperationAuthority(id);

        return groupRepository.findWithLockingById(id).orElseThrow(() -> new GroupNotFoundException(id));
    }
}
