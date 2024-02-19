package smartfloor.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import smartfloor.domain.Role;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.UserMeasurementType;
import smartfloor.domain.entities.Application;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.entities.UserMeasurement;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.exception.CannotUnarchiveUserException;
import smartfloor.domain.exception.GroupUserLimitReachedException;
import smartfloor.domain.exception.TenantNotFoundException;
import smartfloor.domain.exception.TenantUserLimitReachedException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserMeasurementNotFoundException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.ApplicationRepository;
import smartfloor.repository.jpa.UserMeasurementRepository;
import smartfloor.repository.jpa.UserRepository;

/**
 * Logic related to user management.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMeasurementRepository userMeasurementRepository;
    private final TenantService tenantService;
    private final UserWearableLinkService userWearableLinkService;
    private final GroupService groupService;
    private final AuthorizationService authorizationService;
    private final ApplicationRepository applicationRepository;

    /**
     * TODO.
     */
    @Autowired
    public UserService(
            UserRepository userRepository,
            UserMeasurementRepository userMeasurementRepository,
            TenantService tenantService,
            UserWearableLinkService userWearableLinkService,
            GroupService groupService,
            AuthorizationService authorizationService,
            ApplicationRepository applicationRepository
    ) {
        this.userRepository = userRepository;
        this.userMeasurementRepository = userMeasurementRepository;
        this.tenantService = tenantService;
        this.userWearableLinkService = userWearableLinkService;
        this.groupService = groupService;
        this.authorizationService = authorizationService;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Retrieve a list of all users by evaluating the permissions for a given authenticated user.
     * If the authenticated user is:
     * 1. A tenant admin: all users of the tenant are returned.
     * 2. A group manager: all users of the groups managed by the authenticated user are returned.
     * 3. A regular user: only the authenticated user is returned.
     *
     * @return a list of all users that the given authenticated user has access to
     */
    public List<User> getUsers() {
        UserDetails authenticated = authorizationService.getAuthenticatedUser()
                .orElseThrow(() ->
                        new AccessDeniedException("Failed to validate (authenticated) user authorizations."));
        Collection<? extends GrantedAuthority> authorities = authenticated.getAuthorities();
        boolean isAdmin = authorities.contains(Role.ADMIN.toGrantedAuthority());
        boolean isManager = authorities.contains(Role.MANAGER.toGrantedAuthority());
        boolean isComposite = authenticated instanceof CompositeUser;

        if (isAdmin) {
            return userRepository.findAll();
        } else if (isManager) {
            User authenticatedUser = (User) authenticated;
            return Stream.concat(
                            Stream.of(authenticatedUser),
                            authenticatedUser
                                    .getManagedGroups()
                                    .stream()
                                    .map(Group::getUsers)
                                    .flatMap(List::stream)
                    )
                    .toList();
        } else if (isComposite) {
            CompositeUser authenticatedUser = (CompositeUser) authenticated;
            return userRepository.findByCompositeUserId(authenticatedUser.getId());
        } else {
            return List.of((User) authenticated);
        }
    }

    /**
     * Retrieve the user with the provided identifier.
     *
     * @param userId the identifier of the user to retrieve
     * @return the user with the provided identifier
     * @throws UserNotFoundException if no user with the provided identifier exists
     * @throws AccessDeniedException if the authenticated user is not allowed to retrieve the user with the provided
     *                               identifier
     */
    public User getUser(Long userId) throws UserNotFoundException {
        authorizationService.validateUserOperationAuthority(userId);

        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Retrieve the user with the provided authentication identifier (auth (uu)id).
     */
    public User getUserByAuthId(String authId) throws UserNotFoundException {
        User user = userRepository.findByAuthId(authId).orElseThrow(() -> new UserNotFoundException(
                String.format("User with Cognito UUID %s not found.", authId)
        ));

        authorizationService.validateUserOperationAuthority(user);

        return user;
    }

    /**
     * Update the user with the provided identifier, based on the provided user object.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public User updateUser(User user) throws UserIsArchivedException {
        authorizationService.validateUserOperationAuthority(user);

        if (user.isArchived()) {
            throw new UserIsArchivedException();
        }

        return userRepository.save(user);
    }

    /**
     * Remove the user with the provided identifier.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) throws UserNotFoundException {
        authorizationService.validateUserOperationAuthority(userId);

        User user = getUser(userId);
        userRepository.delete(user);
    }

    /**
     * TODO.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public User createWearableUserWithInfo(Group group, User authenticatedUser, UserInfo info)
            throws TenantNotFoundException, TenantUserLimitReachedException, GroupUserLimitReachedException {
        authorizationService.validateGroupOperationAuthority(group);

        ensureUserLimitsNotExceededForGroupAndTenant(authenticatedUser, group);
        Tenant tenant = tenantService.getTenantById(AccessScopeContext.INSTANCE.getTenantId());
        User user = new User(tenant, new ArrayList<>());
        if (info != null) {
            user.setInfo(info);
        }
        return userRepository.save(user);
    }

    /**
     * TODO.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public User createWearableUser(Group group, User authenticatedUser)
            throws TenantNotFoundException, TenantUserLimitReachedException, GroupUserLimitReachedException {
        authorizationService.validateGroupOperationAuthority(group);

        ensureUserLimitsNotExceededForGroupAndTenant(authenticatedUser, group);
        Tenant tenant = tenantService.getTenantById(AccessScopeContext.INSTANCE.getTenantId());
        User user = new User(tenant, new ArrayList<>());
        return userRepository.save(user);
    }

    /**
     * Return all the measurements recorded for the given user. Soft deleted measurements are filtered out.
     * Note: returns every measurement, also the ones not recorded by the authenticated user perse.
     *
     * @param user the user whose measurements should be returned
     * @return a list of all measurements recorded for the given user
     */
    public List<UserMeasurement> getNonDeletedMeasurements(User user) {
        authorizationService.validateUserOperationAuthority(user);
        List<UserMeasurement> measurements = userMeasurementRepository.findByUserIdAndDeleted(user.getId(), false);
        return measurements.stream()
                .filter(m -> (m.getType() == UserMeasurementType.FALL_INCIDENT && m.getValue() > 0) ||
                        m.getType() == UserMeasurementType.POMA)
                .toList();
    }

    /**
     * Return a user's measurements of a given type for a provided time window.
     */
    public List<UserMeasurement> getNonDeletedMeasurementsOfUserAndTypeWithinTimeWindow(
            User user,
            UserMeasurementType type,
            TimeWindow timeWindow
    ) {
        authorizationService.validateUserOperationAuthority(user);

        List<UserMeasurement> measurements =
                userMeasurementRepository.findByUserIdAndTypeAndDeletedAndRecordedAtBetweenOrderByRecordedAtAsc(
                        user.getId(),
                        type,
                        false,
                        timeWindow.getBeginTime(),
                        timeWindow.getEndTime()
                );
        if (type == UserMeasurementType.FALL_INCIDENT) {
            return measurements.stream().filter(m -> m.getValue() > 0).toList();
        }
        return measurements;
    }

    /**
     * Return a specific user measurement given its id. Soft deleted measurements are filtered out.
     *
     * @param id the unique id of the user measurement
     * @return the user measurement for which the id has been provided
     */
    public UserMeasurement getNonDeletedMeasurementById(Long id) throws UserMeasurementNotFoundException {
        UserMeasurement measurement = userMeasurementRepository.findByIdAndDeleted(id, false).orElseThrow(
                () -> new UserMeasurementNotFoundException(id)
        );

        authorizationService.validateUserOperationAuthority(measurement.getUser());

        return measurement;
    }

    /**
     * Record a new user measurement.
     *
     * @param userMeasurement the user measurement (also contains the user for which the measurement is to be recorded)
     * @return the persisted user measurement
     */
    public UserMeasurement recordMeasurement(UserMeasurement userMeasurement) throws UserIsArchivedException {
        authorizationService.validateUserOperationAuthority(userMeasurement.getUser());

        if (userMeasurement.getUser().isArchived()) {
            throw new UserIsArchivedException();
        }

        return userMeasurementRepository.save(userMeasurement);
    }

    /**
     * Soft delete a measurement by setting its 'deleted' property to true.
     * WARNING: This method relies on a different service method that has the necessary authorization checks in place.
     */
    public void softDeleteMeasurement(Long measurementId)
            throws UserMeasurementNotFoundException, UserIsArchivedException {
        UserMeasurement userMeasurement = getNonDeletedMeasurementById(measurementId);
        if (userMeasurement.getUser().isArchived()) {
            throw new UserIsArchivedException();
        }
        userMeasurementRepository.softDelete(measurementId);
    }

    /**
     * Archive a user and break their active UWL if such exists.
     */
    public User archiveUser(Long userId) throws UserNotFoundException {
        authorizationService.validateUserOperationAuthority(userId);

        User user = getUser(userId);
        UserWearableLink activeUserWearableLink = userWearableLinkService.getActiveByUser(user).orElse(null);
        boolean hasActiveUserWearableLink = activeUserWearableLink != null;
        if (hasActiveUserWearableLink) {
            activeUserWearableLink.complete();
            userWearableLinkService.updateUserWearableLink(activeUserWearableLink);
        }
        user.archive();
        return userRepository.save(user);
    }

    /**
     * Unarchive a user if a week has passed since archiving them. Check group and tenant limits in the process.
     */
    public User unarchiveUser(Long userId) throws
            CannotUnarchiveUserException,
            UserNotFoundException,
            TenantUserLimitReachedException,
            GroupUserLimitReachedException {
        authorizationService.validateUserOperationAuthority(userId);

        User userToUnarchive = getUser(userId);
        // Assuming only wearable users can be archived
        List<Group> userGroups = userToUnarchive.getGroups();
        for (Group group : userGroups) {
            ensureUserLimitsNotExceededForGroupAndTenant(userToUnarchive, group);
        }
        int allowUnarchivingAfterWeeks = 1;
        LocalDateTime archiveAt = userToUnarchive.getArchivedAt();
        boolean enoughTimePassedSinceArchiving = false;
        if (archiveAt != null) {
            enoughTimePassedSinceArchiving = archiveAt
                    .plusWeeks(allowUnarchivingAfterWeeks)
                    .isBefore(LocalDateTime.now());
        }
        if (userToUnarchive.isArchived() && enoughTimePassedSinceArchiving) {
            userToUnarchive.unarchive();
            return userRepository.save(userToUnarchive);
        } else {
            throw new CannotUnarchiveUserException();
        }
    }

    /**
     * Get list of applications accessible to the currently authenticated object - User or CompositeUser.
     * For Composite users, we collect their sub-users accessible applications.
     */
    public List<Application> getAccessibleApplicationsForUserDetails(UserDetails userDetails) {
        Set<Application> tenantApplications = new HashSet<>();
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            tenantApplications.addAll(applicationRepository.getApplicationsByUser(user));
            tenantApplications.addAll(applicationRepository.getApplicationsByTenant(user.getTenant()));
        } else {
            CompositeUser compositeUser = (CompositeUser) userDetails;
            List<User> subUsers = userRepository.findByCompositeUserId(compositeUser.getId());
            tenantApplications.addAll(subUsers.stream()
                    .map(applicationRepository::getApplicationsByUser)
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList());
            tenantApplications.addAll(subUsers.stream()
                    .map(user -> applicationRepository.getApplicationsByTenant(user.getTenant()))
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList());
        }
        return new ArrayList<>(tenantApplications);
    }

    /**
     * A helper method for checking the user limits of the current tenant and group.
     * Called either when creating a new wearable user, or when unarchiving one.
     */
    private void ensureUserLimitsNotExceededForGroupAndTenant(User user, Group group)
            throws GroupUserLimitReachedException, TenantUserLimitReachedException {
        Integer tenantUserLimit = user.getTenant().getUserLimit();
        Integer groupUserLimit = group.getUserLimit();
        boolean isGroupUserLimitSet = groupUserLimit != null;
        if (isGroupUserLimitSet) {
            if (group.getUsers().size() >= groupUserLimit) {
                throw new GroupUserLimitReachedException(groupUserLimit);
            }
        } else {
            List<Group> groupsOfTenant = groupService.getGroups();
            List<User> usersOfTenantWithoutManagers = groupsOfTenant.stream()
                    .flatMap(tenantGroup -> tenantGroup.getUsers().stream())
                    .toList();
            long currentAmountOfUsers = usersOfTenantWithoutManagers.size();
            boolean isTenantUserLimitSet = tenantUserLimit != null;
            if (isTenantUserLimitSet && currentAmountOfUsers >= tenantUserLimit) {
                throw new TenantUserLimitReachedException(tenantUserLimit);
            }
        }
    }
}
