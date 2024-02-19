package smartfloor.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import smartfloor.domain.Role;
import smartfloor.domain.dto.UserLimit;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.exception.GroupAlreadyExistsException;
import smartfloor.domain.exception.GroupLimitExceededTenantLimitException;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.UserCountExceededNewLimitException;
import smartfloor.repository.jpa.GroupRepository;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GroupService groupService;

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

    private Group getTestGroupWithName(String groupName) {
        Tenant tenant = Tenant.getDefaultTenant();
        List<User> users = new ArrayList<>();
        users.add(User.builder().tenant(tenant).authId("test").build());
        return Group.builder().tenant(tenant).name(groupName).users(users).build();
    }

    @Test
    void testCreateGroup() throws GroupAlreadyExistsException {
        // given
        Group group = getTestGroupWithName("test");
        // when
        when(groupRepository.save(group)).thenReturn(group);
        Group createdGroup = groupService.createGroup(group);
        // then
        assertEquals(group, createdGroup);
    }

    @Test
    void testCreateGroupWithExistingName() throws GroupAlreadyExistsException {
        // given
        String groupName = "test";
        Group group = getTestGroupWithName(groupName);
        Group existingGroupWithSameName = getTestGroupWithName(groupName);
        // when
        when(groupRepository.existsByTenantAndName(
                existingGroupWithSameName.getTenant(),
                existingGroupWithSameName.getName()
        ))
                .thenReturn(true);
        // then a GroupAlreadyExistsException is thrown
        assertThrows(GroupAlreadyExistsException.class, () -> groupService.createGroup(group));
    }

    @Test
    void testUpdateGroup() throws GroupAlreadyExistsException, GroupNotFoundException {
        // given
        Group group = getTestGroupWithName("test");
        group.setId(1L);
        // when
        when(groupRepository.existsById(group.getId())).thenReturn(true);
        when(groupRepository.existsByIdNotAndTenantAndName(
                group.getId(),
                group.getTenant(),
                group.getName()
        )).thenReturn(false);
        when(groupRepository.save(group)).thenReturn(group);
        Group updatedGroup = groupService.updateGroup(group);
        // then
        assertEquals(updatedGroup, group);
    }

    @Test
    void testUpdateNonExistingGroup() {
        // given
        Group group = getTestGroupWithName("test");
        group.setId(1L);
        // when
        when(groupRepository.existsById(group.getId())).thenReturn(false);
        // then a GroupNotFoundException is thrown
        assertThrows(GroupNotFoundException.class, () -> groupService.updateGroup(group));
    }

    @Test
    void testUpdateGroupNameToExistingName() {
        // given
        Group group = getTestGroupWithName("test");
        group.setId(1L);
        // when
        when(groupRepository.existsById(group.getId())).thenReturn(true);
        when(groupRepository.existsByIdNotAndTenantAndName(
                group.getId(),
                group.getTenant(),
                group.getName()
        )).thenReturn(true);
        // then a GroupAlreadyExistsException is thrown
        assertThrows(GroupAlreadyExistsException.class, () -> groupService.updateGroup(group));
    }

    @Test
    void testGetGroupsForRegularUser() {
        // given
        setupSecurityContext();
        Group group = getTestGroupWithName("test");
        List<Group> groups = List.of(group);
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).groups(groups).build();
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(groupRepository.findGroupsByUser(authenticatedUser)).thenReturn(groups);
        List<Group> foundGroups = groupService.getGroups();
        // then
        assertEquals(authenticatedUser.getGroups(), foundGroups);
    }

    @Test
    void testGetGroupsForAdminUser() {
        // given
        setupSecurityContext();
        Group group = getTestGroupWithName("test");
        List<Group> groups = List.of(group);
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).managedGroups(groups).build();
        Collection<? extends GrantedAuthority> authorities = List.of(Role.ADMIN.toGrantedAuthority());
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(groupRepository.findAll()).thenReturn(groups);
        List<Group> foundGroups = groupService.getGroups();
        // then
        assertEquals(groups, foundGroups);
    }

    @Test
    void testGetGroupsForManagerUser() {
        // given
        setupSecurityContext();
        Group group = getTestGroupWithName("test");
        List<Group> groups = List.of(group);
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).managedGroups(groups).build();
        Collection<? extends GrantedAuthority> authorities = List.of(Role.MANAGER.toGrantedAuthority());
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(groupRepository.findGroupsByManager(authenticatedUser)).thenReturn(groups);
        List<Group> foundGroups = groupService.getGroups();
        // then
        assertEquals(authenticatedUser.getManagedGroups(), foundGroups);
    }

    @Test
    void testGetById() throws GroupNotFoundException {
        // given
        long groupId = 1L;
        Group group = getTestGroupWithName("test");
        group.setId(groupId);
        // when
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        Group foundGroup = groupService.getGroup(groupId);
        // then
        assertEquals(group, foundGroup);
    }

    @Test
    void testGetNonExistingGroup() {
        // given
        long groupId = 1L;
        Group group = getTestGroupWithName("test");
        group.setId(groupId);
        // when, then a GroupNotFoundException is thrown
        assertThrows(GroupNotFoundException.class, () -> groupService.getGroup(groupId));
    }

    @Test
    void testSetLimitSmallerThanUserCount() {
        // given
        setupSecurityContext();
        Group group = getTestGroupWithName("test");
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).build();
        Collection<? extends GrantedAuthority> authorities = List.of(Role.ADMIN.toGrantedAuthority());
        List<Group> groups = List.of(group);
        UserLimit userLimit = new UserLimit(0);
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(groupRepository.findAll()).thenReturn(groups);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        // then a UserCountExceededNewLimitException is thrown
        assertThrows(
                UserCountExceededNewLimitException.class,
                () -> groupService.updateGroupUserLimit(authenticatedUser, group.getId(), userLimit)
        );
    }

    @Test
    void testSetLimitHigherThanOtherGroupsSize() {
        // given
        setupSecurityContext();
        Tenant tenant = Tenant.getDefaultTenant();
        tenant.setUserLimit(1);
        User user = User.builder().tenant(tenant).authId("test").build();
        List<User> users = List.of(user);
        Group group1 = Group.builder().tenant(tenant).name("testGroup1").users(users).build();
        group1.setId(1L);
        Group group2 = Group.builder().tenant(tenant).name("testGroup2").users(List.of()).build();
        group2.setId(2L);
        List<Group> groups = List.of(group1, group2);
        User authenticatedUser = User.builder().tenant(tenant).build();
        Collection<? extends GrantedAuthority> authorities = List.of(Role.ADMIN.toGrantedAuthority());
        UserLimit userLimit = new UserLimit(1);
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(groupRepository.findAll()).thenReturn(groups);
        when(groupRepository.findById(group2.getId())).thenReturn(Optional.of(group2));
        // then a GroupLimitExceededTenantLimitException is thrown
        assertThrows(
                GroupLimitExceededTenantLimitException.class,
                () -> groupService.updateGroupUserLimit(authenticatedUser, group2.getId(), userLimit)
        );
    }

    @Test
    void testSetLimitHigherThanOtherGroupsLimit() {
        // given
        setupSecurityContext();
        Tenant tenant = Tenant.getDefaultTenant();
        tenant.setUserLimit(1);
        Group group1 = Group.builder().tenant(tenant).name("testGroup1").users(List.of()).userLimit(1).build();
        group1.setId(1L);
        Group group2 = Group.builder().tenant(tenant).name("testGroup2").users(List.of()).build();
        group2.setId(2L);
        List<Group> groups = List.of(group1, group2);
        User authenticatedUser = User.builder().tenant(tenant).build();
        Collection<? extends GrantedAuthority> authorities = List.of(Role.ADMIN.toGrantedAuthority());
        UserLimit userLimit = new UserLimit(1);
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(groupRepository.findAll()).thenReturn(groups);
        when(groupRepository.findById(group2.getId())).thenReturn(Optional.of(group2));
        // then a GroupLimitExceededTenantLimitException is thrown
        assertThrows(
                GroupLimitExceededTenantLimitException.class,
                () -> groupService.updateGroupUserLimit(authenticatedUser, group2.getId(), userLimit)
        );
        Mockito.verify(authorizationService, atLeast(1)).validateGroupOperationAuthority(group2.getId());
    }

    @Test
    void testSetLimit() throws
            GroupAlreadyExistsException,
            GroupLimitExceededTenantLimitException,
            GroupNotFoundException,
            UserCountExceededNewLimitException {
        // given
        setupSecurityContext();
        Group group = getTestGroupWithName("test");
        group.setId(1L);
        User authenticatedUser = User.builder().tenant(Tenant.getDefaultTenant()).build();
        Collection<? extends GrantedAuthority> authorities = List.of(Role.ADMIN.toGrantedAuthority());
        List<Group> groups = List.of(group);
        UserLimit userLimit = new UserLimit(1);
        // when
        when(authorizationService.getAuthenticatedUser()).thenReturn(Optional.of(authenticatedUser));
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(groupRepository.findAll()).thenReturn(groups);
        when(groupRepository.existsById(group.getId())).thenReturn(true);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupRepository.save(group)).thenReturn(group);
        UserLimit result = groupService.updateGroupUserLimit(authenticatedUser, group.getId(), userLimit);
        // then
        assertEquals(userLimit.getValue(), result.getValue());
    }
}
