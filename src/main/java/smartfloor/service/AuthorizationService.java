package smartfloor.service;

import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import smartfloor.domain.Role;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.UserRepository;

/**
 * This class provides authorization checks for the service layer.
 */
@Service
public class AuthorizationService {
    private static final String FAILED_TO_VALIDATE_AUTHENTICATED_USER_AUTHORIZATIONS =
            "Failed to validate (authenticated) user authorizations.";
    private final UserRepository userRepository;
    private final CompositeUserRepository compositeUserRepository;

    @Autowired
    public AuthorizationService(UserRepository userRepository, CompositeUserRepository compositeUserRepository) {
        this.userRepository = userRepository;
        this.compositeUserRepository = compositeUserRepository;
    }

    /**
     * Many methods in the service layer are executing a certain operation on a given user (id).
     * This method is used to validate that the authenticated user is allowed to execute the operation.
     * They should be either a (tenant) admin, a manager of the (group of the) user, or the user themselves.
     * For (group) managers, it provides a more fine-grained authorization check than the @PreAuthorize annotation.
     * This since it checks if an authenticated user manages a user specifically (instead of merely managing any
     * group of users).
     *
     * @param userId the id of the user that is being operated on
     * @throws AccessDeniedException if the authenticated user is not allowed to execute the operation on the given user
     */
    public void validateUserOperationAuthority(Long userId) throws AccessDeniedException {
        UserDetails authenticated = getAuthenticatedUser()
                .orElseThrow(() ->
                        new AccessDeniedException(FAILED_TO_VALIDATE_AUTHENTICATED_USER_AUTHORIZATIONS));
        if (authenticated instanceof User) {
            User authenticatedUser = (User) authenticated;
            boolean isRequestingSelf = authenticatedUser.getId().equals(userId);
            Collection<? extends GrantedAuthority> authorities = authenticatedUser.getAuthorities();
            boolean isAdmin = authorities.contains(Role.ADMIN.toGrantedAuthority());
            boolean isManager = authorities.contains(Role.MANAGER.toGrantedAuthority()) &&
                    userRepository.isManagingUser(authenticatedUser.getId(), userId);

            if (!(isAdmin || isRequestingSelf || isManager)) {
                throw new AccessDeniedException(
                        String.format(
                                "User %d is not allowed to perform this operation for user %d.",
                                authenticatedUser.getId(),
                                userId
                        )
                );
            }
        } else if (authenticated instanceof CompositeUser) {
            CompositeUser authenticatedCU = (CompositeUser) authenticated;
            boolean isRequestingSubUser = userRepository
                    .findByCompositeUserId(authenticatedCU.getId())
                    .stream()
                    .map(User::getId)
                    .toList().contains(userId);
            if (!isRequestingSubUser) {
                throw new AccessDeniedException(
                        String.format(
                                "Composite user %d is not allowed to perform this operation for user %d.",
                                authenticatedCU.getId(),
                                userId
                        )
                );
            }
        }
    }

    /**
     * Many methods in the service layer are executing a certain operation on a given user (id).
     * This method is used to validate that the authenticated user is allowed to execute the operation.
     * They should be either a (tenant) admin, a manager of the (group of the) user, or the user themselves.
     * For (group) managers, it provides a more fine-grained authorization check than the @PreAuthorize annotation.
     * This since it checks if an authenticated user manages a user specifically (instead of merely managing any
     * group of users).
     *
     * @param user the user that is being operated on
     * @throws AccessDeniedException if the authenticated user is not allowed to execute the operation on the given user
     */
    public void validateUserOperationAuthority(User user) throws AccessDeniedException {
        validateUserOperationAuthority(user.getId());
    }

    /**
     * Many methods in the service layer are executing a certain operation on a given group (id).
     * This method is used to validate that the authenticated user is allowed to execute the operation.
     * They should be either a (tenant) admin or a manager of the group.
     * For (group) managers, it provides a more fine-grained authorization check than the @PreAuthorize annotation.
     * This since it checks if an authenticated user manages a group specifically (instead of merely managing any
     * group of users).
     *
     * @param groupId the id of the group that is being operated on
     * @throws AccessDeniedException if the authenticated user is not allowed to execute the operation on the given
     *                               group
     */
    public void validateGroupOperationAuthority(Long groupId) throws AccessDeniedException {
        UserDetails authenticated = getAuthenticatedUser()
                .orElseThrow(() ->
                        new AccessDeniedException(FAILED_TO_VALIDATE_AUTHENTICATED_USER_AUTHORIZATIONS));

        if (authenticated instanceof User) {
            User authenticatedUser = (User) authenticated;

            Collection<? extends GrantedAuthority> authorities = authenticatedUser.getAuthorities();
            boolean isAdmin = authorities.contains(Role.ADMIN.toGrantedAuthority());
            boolean isManager = authorities.contains(Role.MANAGER.toGrantedAuthority()) &&
                    userRepository.isManagingGroup(authenticatedUser.getId(), groupId);

            if (!(isAdmin || isManager)) {
                throw new AccessDeniedException(
                        String.format(
                                "User %d is not allowed to perform this operation for group %d.",
                                authenticatedUser.getId(),
                                groupId
                        )
                );
            }
        } else if (authenticated instanceof CompositeUser) {
            CompositeUser authenticatedCU = (CompositeUser) authenticated;
            Collection<? extends GrantedAuthority> authorities = authenticatedCU.getAuthorities();
            // Technically, we shouldn't have composite users, whose role is not a USER, but we still check the role.
            boolean isRegularUser = authorities.contains(Role.USER.toGrantedAuthority());
            if (isRegularUser) {
                throw new AccessDeniedException(
                        String.format(
                                "Composite user %d is not allowed to perform this operation for group %d.",
                                authenticatedCU.getId(),
                                groupId
                        )
                );
            }
        }
    }

    /**
     * Many methods in the service layer are executing a certain operation on a given group (id).
     * This method is used to validate that the authenticated user is allowed to execute the operation.
     * They should be either a (tenant) admin or a manager of the group.
     * For (group) managers, it provides a more fine-grained authorization check than the @PreAuthorize annotation.
     * This since it checks if an authenticated user manages a group specifically (instead of merely managing any
     * group of users).
     *
     * @param group the group that is being operated on
     * @throws AccessDeniedException if the authenticated user is not allowed to execute the operation on the given
     *                               group
     */
    public void validateGroupOperationAuthority(Group group) throws AccessDeniedException {
        validateGroupOperationAuthority(group.getId());
    }

    /**
     * <p>Some methods in the service layer are executing a certain operation on a given wearable (id).
     * This method is used to validate that the authenticated user is CURRENTLY allowed to access a wearable through
     * checking wearable groups. RLS policies allow access to a wearable even if its lease with the current tenant is
     * terminated. We want to provide further filtering in some cases though. For example, creating a user-wearable
     * link within a tenant shouldn't be allowed in case the tenant's lease with the provided wearable is no longer
     * active.</p>
     * This method also verifies the current user's permissions based on their role. Each role has access to a specific
     * set of wearables.
     * Admins can get any wearable,
     * Group managers can get wearables associated only to the groups they manage,
     * Regular users can get wearables associated only to groups they are a member of.
     *
     * @param wearableId the id of the wearable that is being operated on
     */
    public void validateCurrentWearableOperationAuthority(String wearableId) {
        UserDetails authenticated = getAuthenticatedUser()
                .orElseThrow(() ->
                        new AccessDeniedException(FAILED_TO_VALIDATE_AUTHENTICATED_USER_AUTHORIZATIONS));

        if (authenticated instanceof User) {
            User authenticatedUser = (User) authenticated;
            Collection<? extends GrantedAuthority> authorities = authenticatedUser.getAuthorities();
            boolean isAdmin = authorities.contains(Role.ADMIN.toGrantedAuthority());
            boolean isManager = authorities.contains(Role.MANAGER.toGrantedAuthority());
            boolean isRegularUser = authorities.contains(Role.USER.toGrantedAuthority());

            boolean isAllowedToAccessWearable =
                    (isManager && userRepository.isManagerAllowedToAccessWearable(
                            authenticatedUser.getId(),
                            wearableId
                    )) || (isRegularUser && userRepository.isUserAllowedToAccessWearable(
                            authenticatedUser.getId(),
                            wearableId
                    ));

            if (!(isAdmin || isAllowedToAccessWearable)) {
                throw new AccessDeniedException(
                        String.format(
                                "User %d has no access to wearable %s.",
                                authenticatedUser.getId(),
                                wearableId
                        )
                );
            }
        } else if (authenticated instanceof CompositeUser) {
            CompositeUser authenticatedCU = (CompositeUser) authenticated;
            Collection<? extends GrantedAuthority> authorities = authenticatedCU.getAuthorities();
            boolean isRegularUser = authorities.contains(Role.USER.toGrantedAuthority());
            boolean isAllowedToAccessWearable =
                    isRegularUser && compositeUserRepository.isCUAllowedToAccessWearable(
                            authenticatedCU.getId(),
                            wearableId
                    );
            if (!isAllowedToAccessWearable) {
                throw new AccessDeniedException(
                        String.format(
                                "Composite user %d has no access to wearable %s.",
                                authenticatedCU.getId(),
                                wearableId
                        )
                );
            }
        }
    }

    /**
     * <p>Some methods in the service layer are executing a certain operation on a given wearable (id).
     * This method is used to validate that the authenticated user is CURRENTLY allowed to access a wearable through
     * checking wearable groups. RLS policies allow access to a wearable even if its lease with the current tenant is
     * terminated. We want to provide further filtering in some cases though. For example, creating a user-wearable
     * link within a tenant shouldn't be allowed in case the tenant's lease with the provided wearable is no longer
     * active.</p>
     * This method also verifies the current user's permissions based on their role. Each role has access to a specific
     * set of wearables.
     * Admins can get any wearable,
     * Group managers can get wearables associated only to the groups they manage,
     * Regular users can get wearables associated only to groups they are a member of.
     *
     * @param wearable the wearable that is being operated on
     */
    public void validateCurrentWearableOperationAuthority(Wearable wearable) throws AccessDeniedException {
        validateCurrentWearableOperationAuthority(wearable.getId());
    }

    /**
     * Some methods in the service layer are executing a certain operation on a given tenant (id).
     * This method is used to validate that the authenticated user is allowed to execute the operation.
     * They should be a (tenant) admin and a member of the tenant.
     *
     * @param tenantId the id of the tenant that is being operated on
     */
    public void validateTenantOperationAuthority(Long tenantId) throws AccessDeniedException {
        UserDetails authenticated = getAuthenticatedUser()
                .orElseThrow(() ->
                        new AccessDeniedException(FAILED_TO_VALIDATE_AUTHENTICATED_USER_AUTHORIZATIONS));
        if (authenticated instanceof User) {
            User authenticatedUser = (User) authenticated;
            Collection<? extends GrantedAuthority> authorities = authenticatedUser.getAuthorities();
            boolean isAdmin = authorities.contains(Role.ADMIN.toGrantedAuthority());
            boolean isManager = authorities.contains(Role.MANAGER.toGrantedAuthority());
            boolean isPartOfTenant = authenticatedUser.getTenant().getId().equals(tenantId);

            // We don't want to allow the GM access to their tenant, but it is necessary, so they can check
            // the tenant limit upon user creation.
            // TODO: VIT-1096
            if (!((isAdmin || isManager) && isPartOfTenant)) {
                throw new AccessDeniedException(
                        String.format(
                                "User %d is not allowed to perform this operation for tenant %d.",
                                authenticatedUser.getId(),
                                tenantId
                        )
                );
            }
        } else if (authenticated instanceof CompositeUser) {
            CompositeUser authenticatedCU = (CompositeUser) authenticated;
            Collection<? extends GrantedAuthority> authorities = authenticatedCU.getAuthorities();
            boolean isRegularUser = authorities.contains(Role.USER.toGrantedAuthority());
            if (isRegularUser) {
                throw new AccessDeniedException(
                        String.format(
                                "Composite user %d is not allowed to perform this operation for tenant %d.",
                                authenticatedCU.getId(),
                                tenantId
                        )
                );
            }
        }
    }


    /**
     * Some methods in the service layer are executing a certain operation on a given tenant (id).
     * This method is used to validate that the authenticated user is allowed to execute the operation.
     * They should be a (tenant) admin and a member of the tenant.
     *
     * @param tenant the tenant that is being operated on
     */
    public void validateTenantOperationAuthority(Tenant tenant) throws AccessDeniedException {
        validateTenantOperationAuthority(tenant.getId());
    }

    /**
     * TODO.
     */
    public Optional<UserDetails> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            User user = (User) principal;
            return Optional.of(user);
        }
        if (principal instanceof CompositeUser) {
            CompositeUser compositeuser = (CompositeUser) principal;
            return Optional.of(compositeuser);
        }
        return Optional.empty();
    }
}
