package smartfloor.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import smartfloor.domain.UserType;
import smartfloor.multitenancy.AccessScopeContext;

/**
 * Defines custom (w.r.t. Spring Security) authentication filters for authenticating with Spring Security.
 * It provides two filters:
 * 1. A 'default' filter for authenticating with Spring Security as the user for which the auth id field is equal to the
 * content of the 'userAuthId' HTTP header in the request. Used in production and (integration) test environments.
 * 2. A filter for authenticating as the dev user (hardcoded user id) in a development environment.
 */
public class CustomAuthFilter {

    static class AuthenticationFailureResponse {

        private final String message;

        AuthenticationFailureResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * <p>This custom authentication filter is responsible for authenticating with Spring Security as the user for which
     * the auth id field is equal to the content of the 'userAuthId' HTTP header in the request. The HTTP header is
     * populated beforehand (by the calling service, or fixed in an integration test context).</p>
     * WARNING: This assumes that the REST API exposed by this Spring application is only ever called from an external
     * service proxying the request to this application. The authentication step has been handled by that service, and
     * the user authentication id has been determined and passed on to this application.
     * In production, for example, the user authentication id can be the Cognito UUID obtained after a Cognito
     * authorizer has been invoked on the request's JWT token (i.e. by AWS API Gateway). In that case, API Gateway takes
     * the role of the external service handling the authentication.
     */
    @Profile({"prod", "test"})
    public static class DefaultAuthFilter extends UsernamePasswordAuthenticationFilter {

        /**
         * The request header containing the user's auth id (Cognito UUID).
         * The auth id is obtained from the JWT token by API Gateway after a Cognito authorizer has been invoked.
         */
        public static final String USER_AUTH_ID_REQUEST_HEADER = "userAuthId";

        private static final Logger log = LoggerFactory.getLogger(DefaultAuthFilter.class);
        private final ObjectMapper objectMapper = new ObjectMapper();

        public DefaultAuthFilter(AuthenticationManager authenticationManager) {
            this.setAuthenticationManager(authenticationManager);
        }

        /**
         * Handle authentication based on the user auth id we obtain from the request header.
         * Note:  We set credentials as an empty (password) string since this will allow us to use the default
         * authentication manager for the custom UserDetailsService we defined. The password does not hold any meaning
         * as authentication has already been handled by AWS Cognito. See SecurityConfig class for more information.
         *
         * @param userAuthId The user auth id (Cognito id) we obtain from the request header
         */
        private void handleAuthentication(String userAuthId) throws AuthenticationServiceException {
            Authentication auth = new UsernamePasswordAuthenticationToken(userAuthId, "");
            auth = super.getAuthenticationManager().authenticate(auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
            if (AccessScopeContext.INSTANCE.getUserType() == UserType.DIRECT_USER) {
                log.debug(
                        "Successfully authenticated user with auth id {} for tenant {}.",
                        userAuthId,
                        AccessScopeContext.INSTANCE.getTenantId()
                );
            } else {
                log.debug(
                        "Successfully authenticated user with auth id {} and composite user id {}.",
                        userAuthId,
                        AccessScopeContext.INSTANCE.getCompositeUserId()
                );
            }
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            /* #275: We extract the user auth id (Cognito UUID) from the request header set by API gateway after Cognito
            authorization and authenticate with Spring Security. */
            String userAuthIdRequestHeader = request.getHeader(USER_AUTH_ID_REQUEST_HEADER);
            UserType userType = AccessScopeContext.INSTANCE.getUserType();
            if (userAuthIdRequestHeader != null) {
                try {
                    handleAuthentication(userAuthIdRequestHeader);
                    super.doFilter(req, res, chain);
                } catch (AuthenticationServiceException e) {
                    String authFailureMessage = "";
                    if (userType == UserType.DIRECT_USER) {
                        long tenantId = AccessScopeContext.INSTANCE.getTenantId();
                        /* We log at the info level because there is at least one use case where this may occur:
                            A superuser that has switched tenants but still had an active token in the old tenant
                            (see VIT-739).*/
                        log.info("Authentication failed for user with auth id {}, tenant id: {}. Exception message: {}",
                                userAuthIdRequestHeader, tenantId, e.getMessage()
                        );
                        authFailureMessage = String.format(
                                "Authentication failed for user with user authentication" +
                                        " id %s for tenant with id: %d. Please verify that the provided user" +
                                        " authentication id and tenant id are valid. ",
                                userAuthIdRequestHeader,
                                tenantId
                        );
                    } else {
                        long compositeUserId = AccessScopeContext.INSTANCE.getCompositeUserId();
                        log.info(
                                "Authentication failed for user with auth id {}, composite user id: {}. " +
                                        "Exception message: {}",
                                userAuthIdRequestHeader,
                                compositeUserId,
                                e.getMessage()
                        );
                        authFailureMessage = String.format(
                                "Authentication failed for user with user authentication" +
                                        " id %s for composite user with id: %s. Please verify that the provided user" +
                                        " authentication id and composite user id are valid. ",
                                userAuthIdRequestHeader,
                                compositeUserId
                        );
                    }
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter()
                            .print(objectMapper
                                    .writeValueAsString(new AuthenticationFailureResponse(authFailureMessage)));
                }
            } else {
                if (userType == UserType.DIRECT_USER) {
                    long tenantId = AccessScopeContext.INSTANCE.getTenantId();
                    // Logging at error level because this shouldn't happen with a properly set up proxy request.
                    log.error(
                            "Bad request (400) encountered: header {} not present, tenant id: {}.",
                            USER_AUTH_ID_REQUEST_HEADER,
                            tenantId
                    );
                } else {
                    long compositeUserId = AccessScopeContext.INSTANCE.getCompositeUserId();
                    log.error(
                            "Bad request (400) encountered: header {} not present, composite user id: {}.",
                            USER_AUTH_ID_REQUEST_HEADER,
                            compositeUserId
                    );
                }
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                String userAuthIdMissingMessage = String.format(
                        "Bad request: header '%s' not present.",
                        USER_AUTH_ID_REQUEST_HEADER
                );
                response.getWriter().print(objectMapper.writeValueAsString(
                        new AuthenticationFailureResponse(userAuthIdMissingMessage)
                ));
            }
        }
    }

    /**
     * To simulate an environment as close as possible to production, we inject a dev authentication filter during
     * development. This will let Spring Security know to authenticate as the development user.
     * It is convenient because we no longer need to provide an auth id HTTP header while in development mode.
     */
    @Profile("dev")
    public static class DevAuthFilter extends UsernamePasswordAuthenticationFilter {

        private static final String DEVELOPMENT_USER_AUTH_ID = "smartfloor-dev";
        // See db/migrations/dev/V1__dev_users.sql
        private static final Logger log = LoggerFactory.getLogger(DevAuthFilter.class);

        public DevAuthFilter(AuthenticationManager authenticationManager) {
            this.setAuthenticationManager(authenticationManager);
        }

        /**
         * Authenticate as the development user.
         */
        private void handleAuthentication() {
            Authentication auth = new UsernamePasswordAuthenticationToken(DEVELOPMENT_USER_AUTH_ID, "");
            auth = super.getAuthenticationManager().authenticate(auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("Authenticated as development user {}.", DEVELOPMENT_USER_AUTH_ID);
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {
            handleAuthentication();
            super.doFilter(req, res, chain);
        }
    }
}
