package smartfloor.configuration;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import smartfloor.filter.AccessScopeContextFilter;
import smartfloor.filter.CustomAuthFilter;
import smartfloor.filter.RoleFilter;
import smartfloor.service.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ACTUATOR_HEALTH_CHECK_ENDPOINT = "/actuator/health";

    /**
     * Custom password encoder that just returns true for its matches method. In this way, the additional auth checks
     * done by DaoAuthenticationProvider will always pass (and we can just keep using the default authentication
     * manager). See also ProdCognitoAuthFilter#handleAuthentication.
     */
    private static PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence charSequence) {
                return charSequence.toString();
            }

            @Override
            public boolean matches(CharSequence charSequence, String s) {
                return true;
            }
        };
    }

    /**
     * Spring Security production configuration:
     * - Protects main API routes, requiring the user to be authenticated.
     * - Adds a tenant context filter for extracting the tenant id from the (externally validated) request.
     * - Adds an authentication filter for extracting the user id from the (externally validated) request and
     * authenticating the user within the application.
     */
    @Profile("prod")
    @Configuration
    public static class ProdSecurityConfig {

        private UserDetailsServiceImpl userDetailsServiceImpl;

        private static final Logger log = LoggerFactory.getLogger(ProdSecurityConfig.class);

        /**
         * Default CORS configuration with the additional HTTP method PATCH also being allowed.
         */
        protected CorsConfigurationSource corsConfiguration() {
            CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();
            configuration.addAllowedMethod(HttpMethod.PATCH);
            configuration.addAllowedMethod(HttpMethod.PUT);
            configuration.addAllowedMethod(HttpMethod.DELETE);
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }

        /**
         * Configure an authentication provider.
         */
        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailsService());
            authProvider.setPasswordEncoder(passwordEncoder());
            return authProvider;
        }

        /**
         * Configure an authentication manager, to which we can pass a list of a single AuthenticationProvider
         * (the DaoAuthenticationProvider).
         */
        @Bean
        public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
            return new ProviderManager(authenticationProviders);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return userDetailsServiceImpl;
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return web -> web.ignoring().requestMatchers(ACTUATOR_HEALTH_CHECK_ENDPOINT);
        }

        /**
         * Configures the security filter chain, including settings for Cross-Origin Resource Sharing (CORS), session
         * management (which is disabled in our case), and custom filters.
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfiguration()))
                    .sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer.sessionCreationPolicy(
                            SessionCreationPolicy.NEVER))
                    .authorizeHttpRequests(matcherRegistry -> matcherRegistry.anyRequest().authenticated())
                    .addFilterBefore(cognitoAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(authorizationFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(accessScopeContextFilter(), CustomAuthFilter.DefaultAuthFilter.class);

            http.exceptionHandling(securityExceptionHandlingConfigurer -> securityExceptionHandlingConfigurer
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

            log.info("Spring Security production configuration loaded.");
            return http.build();
        }

        AccessScopeContextFilter.DefaultAccessScopeContextFilter accessScopeContextFilter() {
            return new AccessScopeContextFilter.DefaultAccessScopeContextFilter();
        }

        CustomAuthFilter.DefaultAuthFilter cognitoAuthFilter() {
            return new CustomAuthFilter.DefaultAuthFilter(authenticationManager(List.of(authenticationProvider())));
        }

        RoleFilter.DefaultAuthorizationFilter authorizationFilter() {
            return new RoleFilter.DefaultAuthorizationFilter();
        }

        @Autowired
        public void setUserDetailsServiceImpl(UserDetailsServiceImpl userDetailsServiceImpl) {
            this.userDetailsServiceImpl = userDetailsServiceImpl;
        }
    }

    /**
     * Spring Security development configuration:
     * - Protects main API routes, requiring the user to be authenticated.
     * - Adds a tenant context filter that injects the default Smart Floor tenant context.
     * - Adds an authentication filter that authenticates as the dev user.
     */
    @Profile({"dev"})
    @Configuration
    public static class DevSecurityConfig {

        private UserDetailsServiceImpl userDetailsServiceImpl;

        private static final Logger log = LoggerFactory.getLogger(DevSecurityConfig.class);

        /**
         * Default CORS configuration with the additional HTTP method PATCH also being allowed.
         */
        protected CorsConfigurationSource corsConfiguration() {
            CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();
            configuration.addAllowedMethod(HttpMethod.PATCH);
            configuration.addAllowedMethod(HttpMethod.PUT);
            configuration.addAllowedMethod(HttpMethod.DELETE);
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }

        /**
         * Configure an authentication provider.
         */
        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailsService());
            authProvider.setPasswordEncoder(passwordEncoder());
            return authProvider;
        }

        /**
         * Configure an authentication manager, to which we can pass a list of a single AuthenticationProvider
         * (the DaoAuthenticationProvider).
         */
        @Bean
        public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
            return new ProviderManager(authenticationProviders);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return userDetailsServiceImpl;
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return web -> web.ignoring().requestMatchers(ACTUATOR_HEALTH_CHECK_ENDPOINT);
        }

        /**
         * Configures the security filter chain, including settings for Cross-Origin Resource Sharing (CORS), session
         * management (which is disabled in our case), and custom filters.
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfiguration()))
                    .sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer.sessionCreationPolicy(
                            SessionCreationPolicy.NEVER))
                    .authorizeHttpRequests(matcherRegistry -> matcherRegistry.anyRequest().authenticated())
                    .addFilterBefore(authFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(authorizationFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(accessScopeContextFilter(), CustomAuthFilter.DevAuthFilter.class);

            http.exceptionHandling(securityExceptionHandlingConfigurer -> securityExceptionHandlingConfigurer
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

            log.info("Spring Security development configuration loaded.");
            return http.build();
        }

        AccessScopeContextFilter.DevAccessScopeContextFilter accessScopeContextFilter() {
            return new AccessScopeContextFilter.DevAccessScopeContextFilter();
        }

        CustomAuthFilter.DevAuthFilter authFilter() {
            return new CustomAuthFilter.DevAuthFilter(authenticationManager(List.of(authenticationProvider())));
        }

        RoleFilter.DevAuthorizationFilter authorizationFilter() {
            return new RoleFilter.DevAuthorizationFilter();
        }

        @Autowired
        public void setUserDetailsServiceImpl(UserDetailsServiceImpl userDetailsServiceImpl) {
            this.userDetailsServiceImpl = userDetailsServiceImpl;
        }

    }

    /**
     * Spring Security (integration) test configuration:
     * - Protects main API routes, requiring the user to be authenticated.
     * - Adds a tenant context filter that injects the default Smart Floor tenant context.
     * - Adds an authentication filter that authenticates as the test user.
     */
    @Profile({"test"})
    @Configuration
    public static class TestSecurityConfig {

        private UserDetailsServiceImpl userDetailsServiceImpl;

        private static final Logger log = LoggerFactory.getLogger(TestSecurityConfig.class);

        /**
         * Configure an authentication provider.
         */
        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailsService());
            authProvider.setPasswordEncoder(passwordEncoder());
            return authProvider;
        }

        /**
         * Configure an authentication manager, to which we can pass a list of a single AuthenticationProvider
         * (the DaoAuthenticationProvider).
         */
        @Bean
        public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
            return new ProviderManager(authenticationProviders);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return userDetailsServiceImpl;
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return web -> web.ignoring().requestMatchers(ACTUATOR_HEALTH_CHECK_ENDPOINT);
        }

        /**
         * Configures the security filter chain, including settings for Cross-Origin Resource Sharing (CORS), session
         * management (which is disabled in our case), and custom filters.
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(corsConfigurer -> corsConfigurer
                            .configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues())
                    )
                    .sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer.sessionCreationPolicy(
                            SessionCreationPolicy.NEVER))
                    .authorizeHttpRequests(matcherRegistry -> matcherRegistry.anyRequest().authenticated())
                    .addFilterBefore(authFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(authorizationFilter(), UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(accessScopeContextFilter(), CustomAuthFilter.DefaultAuthFilter.class);

            http.exceptionHandling(securityExceptionHandlingConfigurer -> securityExceptionHandlingConfigurer
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

            log.info("Spring Security (integration) test configuration loaded.");
            return http.build();
        }

        AccessScopeContextFilter.DefaultAccessScopeContextFilter accessScopeContextFilter() {
            return new AccessScopeContextFilter.DefaultAccessScopeContextFilter();
        }

        CustomAuthFilter.DefaultAuthFilter authFilter() {
            return new CustomAuthFilter.DefaultAuthFilter(authenticationManager(List.of(authenticationProvider())));
        }

        RoleFilter.DefaultAuthorizationFilter authorizationFilter() {
            return new RoleFilter.DefaultAuthorizationFilter();
        }

        @Autowired
        public void setUserDetailsServiceImpl(UserDetailsServiceImpl userDetailsServiceImpl) {
            this.userDetailsServiceImpl = userDetailsServiceImpl;
        }
    }
}