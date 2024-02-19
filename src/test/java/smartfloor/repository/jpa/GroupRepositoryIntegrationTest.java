package smartfloor.repository.jpa;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.UserType;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.multitenancy.AccessScopeContext;


class GroupRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    GroupRepository groupRepository;

    private Group getTestGroupWithName(String groupName) {
        List<User> users = new ArrayList<>();
        return Group.builder().tenant(getTestTenant()).name(groupName).users(users).build();
    }

    /**
     * TODO.
     */
    @BeforeEach
    void setUp() {
        AccessScopeContext.INSTANCE.setUserType(UserType.DIRECT_USER);
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());
        groupRepository.deleteAll();
    }

    @Test
    void testExistsByIdNotAndTenantAndName() {
        Group group = getTestGroupWithName("test");
        // when
        group = groupRepository.save(group);
        boolean hasFoundOtherGroup =
                groupRepository.existsByIdNotAndTenantAndName(group.getId(), group.getTenant(), group.getName());
        // then
        assertFalse(hasFoundOtherGroup);
    }

    @Test
    void testExistsByTenantAndName() {
        // given
        Group group = getTestGroupWithName("test");
        // when
        boolean doesGroupExist = groupRepository.existsByTenantAndName(group.getTenant(), group.getName());
        // then
        assertFalse(doesGroupExist);
        // and when
        groupRepository.save(group);
        doesGroupExist = groupRepository.existsByTenantAndName(group.getTenant(), group.getName());
        // then
        assertTrue(doesGroupExist);
    }

}
