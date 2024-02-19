package smartfloor.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.Tenant;
import smartfloor.multitenancy.AccessScopeContext;

/**
 * <p>This request filter is used to filter an incoming request and define the access scope context based on the
 * presence of the headers 'userType' and either 'tenantId' or 'compositeUserId'.</p>
 * WARNING: We will assume the tenant/composite user has been pre-validated by the external service proxying the request
 * to this application. That is, the value of the 'tenantId' and 'compositeUserId' header fields can be trusted
 * (although we still check for odd values where possible).
 * In production, for example, the tenant id value can be obtained from a pre-validated JWT claim.
 * The claim can be obtained from the JWT token obtained after a Cognito authorizer has been invoked on the request's
 * JWT token (i.e. by AWS API Gateway). In that case, API Gateway takes the role of the external service handling the
 * validation.
 */
public class AccessScopeContextFilter {

    private static final String APPLICATION_JSON = "application/json";
    private static final String UTF_8 = "UTF-8";
    private static final String BAD_REQUEST_HEADER_S_NOT_PRESENT = "Bad request: header '%s' not present.";

    static class TenantIdentificationResponseMessage {

        private final String message;

        TenantIdentificationResponseMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @Profile({"prod", "test"})
    public static class DefaultAccessScopeContextFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(DefaultAccessScopeContextFilter.class);
        public static final String USER_TYPE_REQUEST_HEADER = "userType";
        public static final String TENANT_ID_REQUEST_HEADER = "tenantId";
        public static final String COMPOSITE_USER_ID_REQUEST_HEADER = "compositeUserId";
        private final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Extract the user type from the request and, depending on it, set TenantContext value for composite user id
         * or tenant id. We handle exceptional cases such as:
         * - A request header being absent (null value)
         * - A request header containing a non-numeric value
         */
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            // Assuming every user, no matter their role, has this header:
            String userTypeRequestHeader = request.getHeader(USER_TYPE_REQUEST_HEADER);
            String tenantIdRequestHeader = request.getHeader(TENANT_ID_REQUEST_HEADER);
            String compositeUserIdRequestHeader = request.getHeader(COMPOSITE_USER_ID_REQUEST_HEADER);

            if (userTypeRequestHeader == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType(APPLICATION_JSON);
                response.setCharacterEncoding(UTF_8);
                response.getWriter().print(objectMapper.writeValueAsString(
                        new TenantIdentificationResponseMessage(
                                String.format(
                                        BAD_REQUEST_HEADER_S_NOT_PRESENT,
                                        USER_TYPE_REQUEST_HEADER
                                ))));
                return;
            }
            UserType userType = extractUserType(userTypeRequestHeader);
            if (userType == UserType.COMPOSITE_USER) {
                if (compositeUserIdRequestHeader == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.getWriter().print(objectMapper.writeValueAsString(
                            new TenantIdentificationResponseMessage(
                                    String.format(
                                            BAD_REQUEST_HEADER_S_NOT_PRESENT,
                                            COMPOSITE_USER_ID_REQUEST_HEADER
                                    ))));
                    return;
                }
                try {
                    AccessScopeContext.INSTANCE.setUserType(UserType.COMPOSITE_USER);
                    Long compositeUserId = Long.parseLong(compositeUserIdRequestHeader);
                    AccessScopeContext.INSTANCE.setCompositeUserId(compositeUserId);
                    log.debug("Identified as composite user with id {}.", compositeUserId);
                    filterChain.doFilter(request, response);
                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.getWriter().print(objectMapper.writeValueAsString(
                            new TenantIdentificationResponseMessage(
                                    String.format(
                                            "Bad request: header '%s' does not contain a valid number, value: %s.",
                                            COMPOSITE_USER_ID_REQUEST_HEADER,
                                            compositeUserIdRequestHeader
                                    ))));
                }
            } else { // UserType.DIRECT_USER
                if (tenantIdRequestHeader == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.getWriter().print(objectMapper.writeValueAsString(
                            new TenantIdentificationResponseMessage(
                                    String.format(
                                            BAD_REQUEST_HEADER_S_NOT_PRESENT,
                                            TENANT_ID_REQUEST_HEADER
                                    ))));
                    return;
                }
                try {
                    AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
                    Long tenantId = Long.parseLong(tenantIdRequestHeader);
                    AccessScopeContext.INSTANCE.setTenantId(tenantId);
                    log.debug("Identified as tenant {}.", tenantId);
                    filterChain.doFilter(request, response);
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.getWriter().print(objectMapper.writeValueAsString(
                            new TenantIdentificationResponseMessage(
                                    String.format(
                                            "Bad request: header '%s' does not contain a valid number, value: %s.",
                                            TENANT_ID_REQUEST_HEADER,
                                            tenantIdRequestHeader
                                    ))));
                }
            }
        }

        private static UserType extractUserType(String userTypeHeader) {
            if ("COMPOSITE_USER".equals(userTypeHeader)) {
                return UserType.COMPOSITE_USER;
            }
            return UserType.DIRECT_USER;
        }
    }


    /**
     * We simply identify as the default Smart Floor tenant for development purposes.
     */
    @Profile("dev")
    public static class DevAccessScopeContextFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(DevAccessScopeContextFilter.class);

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            Long tenantId = Tenant.getDefaultTenant().getId();
            AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
            AccessScopeContext.INSTANCE.setTenantId(tenantId);
            log.info("Identified as tenant {}.", tenantId);
            filterChain.doFilter(request, response);
        }
    }
}

