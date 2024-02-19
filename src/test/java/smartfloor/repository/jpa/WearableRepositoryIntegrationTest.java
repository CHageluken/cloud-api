package smartfloor.repository.jpa;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;

class WearableRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    WearableRepository wearableRepository;

    @Autowired
    WearableGroupRepository wearableGroupRepository;

    @Autowired
    GroupRepository groupRepository;


    @Test
    void testFindWearable() {
        Tenant tenant = getTestTenant();
        Wearable wearable = new Wearable();
        wearable.setId("testFindWearableW");
        wearable = wearableRepository.save(wearable);

        WearableGroup wearableGroup = WearableGroup.builder()
                .wearables(List.of(wearable))
                .name("testFindWearableWG")
                .build();
        wearableGroup = wearableGroupRepository.save(wearableGroup);

        Group group = Group.builder()
                .name("testFindWearableG")
                .wearableGroup(wearableGroup)
                .tenant(tenant)
                .build();
        group = groupRepository.save(group);

        Wearable wearableFromDB = wearableRepository.findById(wearable.getId());
        assertEquals(wearableFromDB.getId(), wearable.getId());

        groupRepository.delete(group);
        wearableRepository.delete(wearable);
    }
}
