package smartfloor.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import smartfloor.domain.Role;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.repository.jpa.UserRepository;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    /**
     * For some methods, the user must be authenticated with Spring Security.
     * Therefore, we mock the Spring Security context and Authentication object.
     * For the affected tests, this allows us to control the authenticated user and/or the user's role/authorities.
     */
    private void setupSecurityContext() {
        SecurityContextHolder.setContext(securityContext);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void testGetUsersForRegularUser() {
        // given
        setupSecurityContext();
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).build();
        List<User> users = List.of(authenticatedUser);
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        // no need to mock authorities here because the default role is USER
        List<User> actual = userService.getUsers();
        // then
        assertEquals(users, actual);
    }

    @Test
    void testGetUsersForAdminUser() {
        // given
        setupSecurityContext();
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).build();
        User otherUser = User.builder().tenant(Tenant.getDefaultTenant()).authId("otherUser").build();
        List<User> users = List.of(authenticatedUser, otherUser);
        Collection<? extends GrantedAuthority> authorities = List.of(Role.ADMIN.toGrantedAuthority());
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(userRepository.findAll()).thenReturn(users);
        List<User> actual = userService.getUsers();
        // then
        assertEquals(users, actual);
    }

    @Test
    void testGetUsersForGroupManagerUser() {
        // given
        setupSecurityContext();
        User otherUser = User.builder().tenant(Tenant.getDefaultTenant()).authId("otherUser").build();
        Group group = Group.builder().name("testManagedGroup").users(List.of(otherUser)).build();
        List<Group> groups = List.of(group);
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).managedGroups(groups).build();
        List<User> users = List.of(authenticatedUser, otherUser);
        Collection<? extends GrantedAuthority> authorities = List.of(Role.MANAGER.toGrantedAuthority());
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        List<User> actual = userService.getUsers();
        // then
        assertEquals(users, actual);
    }

    @Test
    void testGetUser() throws UserNotFoundException {
        // given
        User user = new User();
        user.setId(1L);
        // when
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        User actual = userService.getUser(user.getId());
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user.getId());
        assertNotNull(actual);
        assertEquals(user, actual);
    }

    @Test
    void testGetNonExistingUser() throws UserNotFoundException {
        Long userId = 1L;
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUser(userId);
        });
    }

    @Test
    void testGetUserByAuthId() throws UserNotFoundException {
        // given
        User user = new User();
        user.setAuthId("test_auth_id");
        // when
        when(userRepository.findByAuthId(user.getAuthId())).thenReturn(Optional.of(user));
        User actual = userService.getUserByAuthId(user.getAuthId());
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user);
        assertNotNull(actual);
        assertEquals(user, actual);
    }

    @Test
    void testGetNonExistingUserByAuthId() throws UserNotFoundException {
        // given
        String authId = "test_auth_id";
        // when
        when(userRepository.findByAuthId(authId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserByAuthId(authId);
        });
    }

    @Test
    void testUpdateUser() throws UserIsArchivedException {
        // given
        User user = new User();
        Long userId = 1L;
        user.setId(userId);
        // when
        when(userRepository.save(user)).thenReturn(user);
        User actual = userService.updateUser(user);
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user);
        assertEquals(user, actual);
    }

    @Test
    void testDeleteUser() throws UserNotFoundException {
        // given
        User user = new User();
        user.setId(1L);
        // when
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        userService.deleteUser(user.getId());
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(user.getId());
        Mockito.verify(userRepository, times(1)).delete(user);
    }

    @Test
    void testDeleteNonExistingUser() throws UserNotFoundException {
        // given
        Long userId = 1L;
        assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(userId);
        });
        // then
        Mockito.verify(authorizationService, atLeast(1)).validateUserOperationAuthority(userId);
        Mockito.verify(userRepository, times(0)).deleteById(userId);
    }

    @Test
    void testGenderFormat() {
        // given
        UserInfo userInfo = new UserInfo();
        String gender = "t";
        userInfo.setGender(gender);
        Assert.assertNotNull(userInfo.getGender());
        Assert.assertEquals("", userInfo.getGender());

        gender = "m";
        userInfo.setGender(gender);
        Assert.assertNotNull(userInfo.getGender());
        Assert.assertEquals("m", userInfo.getGender());
    }

}
