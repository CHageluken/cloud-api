package smartfloor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import smartfloor.domain.UserType;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.CompositeUserRepository;
import smartfloor.repository.jpa.TenantRepository;
import smartfloor.repository.jpa.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CompositeUserRepository compositeUserRepository;

    /**
     * TODO.
     */
    @Autowired
    public UserDetailsServiceImpl(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            CompositeUserRepository compositeUserRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.compositeUserRepository = compositeUserRepository;
    }

    /**
     * <p>Look up the user authentication id (ex. a Cognito UUID) in the database to authenticate an existing user.
     * The access scope context should normally be correct. But we verify this and fail the authentication if it is not
     * (i.e. the tenant/composite user does not exist). Then, based on the UserType stored in the context, we:
     * 1) Verify that the composite user exists the type of the user is COMPOSITE_USER or,
     * 2) Verify that the user exists for the tenant when the user type is DIRECT_USER.
     * If the user (direct or composite) does not exist, we fail the authentication. If the user does exist, we return
     * the user details object.</p>
     * Note: We still assume that the request from which we obtain the user authentication id has been pre-validated.
     *
     * @param userAuthId the user authentication id (ex. a Cognito UUID, hardcoded development/test user id, etc.)
     * @return the user details object describing the user to be authenticated with Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String userAuthId) {
        UserType currentUserType = AccessScopeContext.INSTANCE.getUserType();
        if (currentUserType == UserType.COMPOSITE_USER) {
            // We could look the composite user up either by auth id or id. For consistency, we do it by auth id.
            return compositeUserRepository.findByAuthId(userAuthId).orElseThrow(
                    () -> new AuthenticationServiceException(
                            String.format(
                                    "Composite user with authentication id %s does not exist. Please verify " +
                                            "that the provided authentication id is valid.",
                                    userAuthId
                            )));
        }

        Long tenantId = AccessScopeContext.INSTANCE.getTenantId();

        // We first verify that the tenant from the current access scope context exists. If it does not, we fail
        // the authentication.
        tenantRepository.findById(tenantId).orElseThrow(
                () -> new AuthenticationServiceException(
                        String.format(
                                "Tenant with id %d does not exist. Please verify that the provided tenant id" +
                                        " is valid.",
                                tenantId
                        )));

        // We then look up the user by the provided authentication id.
        return userRepository.findByAuthId(userAuthId).orElseThrow(
                () -> new AuthenticationServiceException(
                        String.format(
                                "User with authentication id %s does not exist for tenant with id: %d. Please verify " +
                                        "that the provided authentication id is valid.",
                                userAuthId,
                                tenantId
                        )));
    }
}
