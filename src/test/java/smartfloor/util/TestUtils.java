package smartfloor.util;


import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import smartfloor.domain.Role;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.filter.AccessScopeContextFilter;
import static smartfloor.filter.AccessScopeContextFilter.DefaultAccessScopeContextFilter.USER_TYPE_REQUEST_HEADER;
import smartfloor.filter.CustomAuthFilter;
import smartfloor.filter.RoleFilter;


public class TestUtils {

    public static final String TEST_USER_AUTH_ID = "smartfloor-test";

    public static String createURLWithPort(String uri, int port) {
        return "http://localhost:" + port + "/" + uri;
    }

    /**
     * The user object for the default test user that can be used as the authenticated user in tests.
     * Note that normally an integration test either looks up this user from the database or creates this user should it
     * have been deleted somewhere along the way.
     *
     * @return the default test user
     */
    public static User testUser() {
        return User
                .builder()
                .tenant(Tenant.getDefaultTenant())
                .authId(TEST_USER_AUTH_ID)
                .build();
    }

    /**
     * The default HTTP headers to be used in tests.
     *
     * @return a HttpHeaders object with the tenant id and auth headers set to the default test user's tenant id and
     * auth id. The role is set to ADMIN, such that every endpoint is accessible.
     */
    public static HttpHeaders defaultHttpHeaders() {
        return new TestHttpHeadersBuilder()
                .withTenantId(testUser().getTenant().getId())
                .withAuthId(testUser().getAuthId())
                .withRole(Role.ADMIN) // default to (tenant) admin such that you have all permissions necessary
                .withUserType(UserType.DIRECT_USER)
                .build();
    }

    /**
     * The default HTTP headers to be used in tests with a custom user role.
     *
     * @param role the role to be used in the test
     * @return a HttpHeaders object with the tenant id and auth headers set to the default test user's tenant id and
     * auth id. The role is set to the specified role.
     */
    public static HttpHeaders defaultHttpHeadersWithRole(Role role) {
        return new TestHttpHeadersBuilder()
                .withTenantId(testUser().getTenant().getId())
                .withAuthId(testUser().getAuthId())
                .withRole(role)
                .withUserType(UserType.DIRECT_USER)
                .build();
    }

    /**
     * TODO.
     */
    public static HttpHeaders httpHeadersBuilder(Long tenantId, User user, Role role, UserType userType) {
        return new TestHttpHeadersBuilder()
                .withTenantId(tenantId)
                .withAuthId(user.getAuthId())
                .withRole(role)
                .withUserType(userType)
                .build();
    }

    /**
     * Custom HTTP headers to be used in tests where a specific authenticated user is desired.
     * The tenant is derived from the user's tenant.
     *
     * @param user the authenticated user to be used in the test
     * @return a HttpHeaders object with the tenant id and auth headers set to the user's tenant id and auth id.
     */
    public static HttpHeaders withAuthenticatedUserHttpHeaders(User user) {
        return new TestHttpHeadersBuilder()
                .withTenantId(user.getTenant().getId())
                .withAuthId(user.getAuthId())
                .withRole(Role.ADMIN) // default to (tenant) admin such that you have all permissions necessary
                .withUserType(UserType.DIRECT_USER)
                .build();
    }

    /**
     * A Builder that allows for specifying custom tenant id and user authentication id headers.
     * These headers always need to be specified in requests during integration tests as they are used to determine the
     * tenant context and the authenticated user.
     */
    public static class TestHttpHeadersBuilder {

        private final HttpHeaders headers;

        public TestHttpHeadersBuilder() {
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        /**
         * TODO.
         */
        public TestHttpHeadersBuilder withTenantId(Long tenantId) {
            headers.add(
                    AccessScopeContextFilter.DefaultAccessScopeContextFilter.TENANT_ID_REQUEST_HEADER,
                    tenantId.toString()
            );
            return this;
        }

        public TestHttpHeadersBuilder withAuthId(String authId) {
            headers.add(CustomAuthFilter.DefaultAuthFilter.USER_AUTH_ID_REQUEST_HEADER, authId);
            return this;
        }

        public TestHttpHeadersBuilder withRole(Role role) {
            headers.add(RoleFilter.DefaultAuthorizationFilter.USER_ROLE_REQUEST_HEADER, role.name());
            return this;
        }

        public TestHttpHeadersBuilder withUserType(UserType userType) {
            headers.add(USER_TYPE_REQUEST_HEADER, userType.name());
            return this;
        }

        public TestHttpHeadersBuilder withCompositeUserId(Long compositeUserId) {
            headers.add("compositeUserId", compositeUserId.toString());
            return this;
        }

        public HttpHeaders build() {
            return headers;
        }
    }
}
