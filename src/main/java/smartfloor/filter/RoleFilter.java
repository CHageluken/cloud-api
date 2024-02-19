package smartfloor.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.multitenancy.AccessScopeContext;

/**
 * Defines a custom authorization filter that extracts the user roles from the request header.
 * These roles have been added to the request header by API Gateway after a Cognito authorizer has been invoked.
 */
public class RoleFilter {

    /**
     * The default authorization filter extracts the roles from the request header and adds them to the
     * Spring Security context. This means that the roles are defined on a per-request basis.
     * We do not persist the roles currently since they are already defined as part of the JWT token (through Cognito).
     */
    @Profile({"prod", "test"})
    public static class DefaultAuthorizationFilter extends OncePerRequestFilter {

        /**
         * The request header containing the user's roles (comma-separated list of strings).
         * The roles are obtained from the JWT token by API Gateway after a Cognito authorizer has been invoked.
         */
        public static final String USER_ROLE_REQUEST_HEADER = "cognitoGroups";

        private static final Logger log = LoggerFactory.getLogger(DefaultAuthorizationFilter.class);

        public DefaultAuthorizationFilter() {

        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String userRolesRequestHeader = request.getHeader(USER_ROLE_REQUEST_HEADER);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (userRolesRequestHeader != null && auth != null && auth.isAuthenticated()) {
                List<GrantedAuthority> authorities = extractAuthorities(userRolesRequestHeader);

                if (authorities.isEmpty()) { // then default to regular user role
                    UserType currentUserType = AccessScopeContext.INSTANCE.getUserType();
                    if (currentUserType == UserType.COMPOSITE_USER) {
                        Long compositeUserId = AccessScopeContext.INSTANCE.getCompositeUserId();
                        log.warn(
                                "No (valid) roles in request for user with auth id {}, composite user id: {}. " +
                                        "Defaulting to regular user role.",
                                userRolesRequestHeader,
                                compositeUserId
                        );
                    } else {
                        Long tenantId = AccessScopeContext.INSTANCE.getTenantId();
                        log.warn(
                                "No (valid) roles in request for user with auth id {}, tenant id: {}. " +
                                        "Defaulting to regular user role.",
                                userRolesRequestHeader,
                                tenantId
                        );
                    }
                    authorities = List.of(Role.USER.toGrantedAuthority());
                }

                Authentication authWithRoles = new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(),
                        auth.getCredentials(),
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authWithRoles);

                chain.doFilter(request, response);
            }
        }

        private static List<GrantedAuthority> extractAuthorities(String cognitoGroups) {
            // extract roles as authorities for Spring Security ignoring any invalid values
            // note: assuming Cognito groups according to agreed (SF) naming convention
            return Arrays.stream(cognitoGroups.split(","))
                    .map(group -> {
                        try {
                            switch (group) {
                                case "TenantAdmin":
                                case "SuperAdmin":
                                case "ADMIN":
                                    return Optional.of(Role.ADMIN);
                                case "GroupManager":
                                case "MANAGER":
                                    return Optional.of(Role.MANAGER);
                                case "User":
                                case "USER":
                                    return Optional.of(Role.USER);
                                default:
                                    return Optional.<Role>empty();
                            }
                        } catch (IllegalArgumentException ex) {
                            log.warn("Invalid role {} in request.", group);
                            return Optional.<Role>empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Role::toGrantedAuthority)
                    .toList();
        }
    }

    /**
     * Default to admin role for dev.
     */
    @Profile("dev")
    public static class DevAuthorizationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Authentication authWithRoles = new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(),
                        auth.getCredentials(),
                        List.of(Role.ADMIN.toGrantedAuthority())
                ); // default to admin role

                SecurityContextHolder.getContext().setAuthentication(authWithRoles);

                chain.doFilter(request, response);
            }
        }
    }

}