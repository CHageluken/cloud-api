package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.multitenancy.AccessScopeContext;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.WearableGroupRepository;
import smartfloor.repository.jpa.WearableRepository;
import smartfloor.util.TestUtils;

class WearableControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    WearableRepository wearableRepository;
    @Autowired
    WearableGroupRepository wearableGroupRepository;
    @Autowired
    GroupRepository groupRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testGetWearable() throws IOException {
        Wearable wearable = new Wearable();
        wearable.setId("testGetWearable");
        wearableRepository.save(wearable);

        WearableGroup wearableGroup = WearableGroup.builder()
                .name("getWearableWG")
                .wearables(List.of(wearable))
                .build();
        wearableGroupRepository.save(wearableGroup);

        Group group = Group.builder()
                .wearableGroup(wearableGroup)
                .tenant(Tenant.getDefaultTenant())
                .name("getWearableG")
                .build();
        groupRepository.save(group);

        HttpEntity<String> entity = new HttpEntity<>(wearable.getId(), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/wearables/" + wearable.getId(), getPort()),
                HttpMethod.GET, entity, String.class
        );
        Wearable wearableFromResponse = mapper.readValue(response.getBody(), Wearable.class);
        assertEquals(wearable.getId(), wearableFromResponse.getId());
    }

    @Test
    void testGetWearables() throws IOException {
        int defaultTenantInitialWearableCount = wearableRepository.findAll().size();
        Tenant tenant = getTestTenant();
        Wearable wearable = new Wearable();
        wearable.setId("testWearableGet");
        wearableRepository.save(wearable);
        WearableGroup wearableGroup = WearableGroup.builder()
                .wearables(List.of(wearable))
                .name("testGetWearable")
                .build();
        wearableGroup = wearableGroupRepository.save(wearableGroup);
        Group group = Group.builder()
                .name("testGetWearable")
                .wearableGroup(wearableGroup)
                .tenant(tenant)
                .build();
        groupRepository.save(group);
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/wearables", getPort()), HttpMethod.GET, entity, String.class);
        List<Wearable> wearablesFromResponse = mapper.readValue(response.getBody(), List.class);
        assertNotEquals(defaultTenantInitialWearableCount, wearablesFromResponse.size());
    }

    @Test
    void testGetWearablesForTenant() throws JsonProcessingException {
        // given: We list all wearables for the default tenant initially, to get their count.
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/wearables", getPort()), HttpMethod.GET, entity, String.class);
        List<Wearable> wearables = mapper.readValue(response.getBody(), new TypeReference<List<Wearable>>() {
        });
        // Note that the wearables endpoint only returns currently leased wearables. The ones with a terminated lease
        // are not included in the response. So, we also get the wearable count straight from the repository, to ensure
        // that RLS policies properly isolate resources for different tenants.
        int defaultTenantInitialWearableCountLeased = wearables.size();
        int defaultTenantInitialWearableCountDB = wearableRepository.findAll().size();

        // then given: We change the tenant in the access scope context to the second one, and we create a wearable
        // for them specifically.
        long secondTenantId = 2L;
        AccessScopeContext.INSTANCE.setTenantId(secondTenantId); // so that we are allowed to create for tenant 2
        int secondTenantInitialWearableCountDB = wearableRepository.findAll().size();
        Tenant secondTenant = tenantRepository.findById(secondTenantId).get();
        Wearable wearable = Wearable.builder().id("testWearable").build();
        wearable = wearableRepository.save(wearable);
        WearableGroup wearableGroup = WearableGroup.builder()
                .wearables(List.of(wearable))
                .name("testGetWearableForTenant")
                .build();
        wearableGroup = wearableGroupRepository.save(wearableGroup);
        Group group = Group.builder()
                .name("testGetWearableForTenant")
                .wearableGroup(wearableGroup)
                .tenant(secondTenant)
                .build();
        groupRepository.save(group);
        int secondTenantFinalWearableCountDB = wearableRepository.findAll().size();
        // Make sure the new wearable for the second tenant has been stored.
        assertEquals(secondTenantFinalWearableCountDB, secondTenantInitialWearableCountDB + 1);

        // then when: We try listing all wearables for the default tenant again.
        AccessScopeContext.INSTANCE.setTenantId(Tenant.getDefaultTenant().getId());
        entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort("/wearables", getPort()), HttpMethod.GET, entity, String.class);

        // then: The default tenant's wearables should still be the same amount.
        wearables = mapper.readValue(response.getBody(), new TypeReference<List<Wearable>>() {
        });
        int defaultTenantFinalWearableCountDB = wearableRepository.findAll().size();
        int defaultTenantFinalWearableCountLeased = wearables.size();
        assertEquals(defaultTenantInitialWearableCountLeased, defaultTenantFinalWearableCountLeased);
        assertEquals(defaultTenantInitialWearableCountDB, defaultTenantFinalWearableCountDB);
    }
}
