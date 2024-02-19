package smartfloor.service;

import java.util.List;
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
import org.springframework.security.core.context.SecurityContext;
import smartfloor.domain.dto.CreateWearableForm;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.domain.exception.WearableAlreadyExistsException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.repository.jpa.WearableRepository;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class WearableServiceTest {

    @Mock
    private WearableRepository wearableRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private WearableService wearableService;

    private Wearable buildWearable() {
        Tenant tenant = new Tenant(Tenant.DEFAULT_TENANT_NAME);
        tenant.setId(1L);
        Wearable wearable = new Wearable();
        wearable.setId("test_wearable");
        return wearable;
    }

    @Test
    void testCreateWearable() throws WearableAlreadyExistsException {
        // given
        Wearable wearable = buildWearable();
        CreateWearableForm createWearableForm = new CreateWearableForm(wearable.getId());
        // when
        Mockito.when(wearableRepository.findById(wearable.getId())).thenReturn(null);
        Mockito.when(wearableRepository.save(wearable)).thenReturn(wearable);
        Wearable actual = wearableService.createWearable(wearable);
        // then
        assertNotNull(actual);
        assertEquals(wearable, actual);
    }

    @Test
    void testCreateWearableWithExistingId() throws WearableAlreadyExistsException {
        // given
        Wearable wearable = buildWearable();
        // when
        Mockito.when(wearableRepository.findById(wearable.getId())).thenReturn(new Wearable());
        assertThrows(WearableAlreadyExistsException.class, () -> {
            wearableService.createWearable(wearable);
        });
    }

    @Test
    void testGetWearableById() throws WearableNotFoundException {
        // given
        Wearable wearable = buildWearable();
        // when
        Mockito.when(wearableRepository.findById(wearable.getId())).thenReturn(wearable);
        Wearable actual = wearableService.getWearable(wearable.getId());
        // then
        assertNotNull(actual);
        assertEquals(wearable, actual);
    }

    @Test
    void testGetNonExistingWearableById() throws WearableNotFoundException {
        // given
        String nonExistingWearableId = "test_wearable";
        // when
        Mockito.when(wearableRepository.findById(nonExistingWearableId)).thenReturn(null);
        // then
        assertThrows(WearableNotFoundException.class, () -> {
            wearableService.getWearable(nonExistingWearableId);
        });
    }

    @Test
    void testGetWearablesForUser() {
        // given
        Wearable first = Wearable.builder().id("first").build();
        Wearable second = Wearable.builder().id("second").build();
        List<Wearable> wearables = List.of(first, second);
        WearableGroup wearableGroup = WearableGroup.builder().wearables(wearables).build();
        Group group = Group.builder().wearableGroup(wearableGroup).build();
        // when
        when(groupService.getGroups()).thenReturn(List.of(group));
        // then
        List<Wearable> actual = wearableService.getWearables();
        assertEquals(wearables, actual);
    }

    @Test
    void testUpdateWearable() throws WearableNotFoundException {
        // given
        Wearable wearable = buildWearable();
        // when
        Mockito.when(wearableRepository.save(wearable)).thenReturn(wearable);
        Wearable actual = wearableService.updateWearable(wearable);
        // then
        assertNotNull(actual);
        assertEquals(wearable, actual);
        Mockito.verify(authorizationService, atLeast(1)).validateCurrentWearableOperationAuthority(
                wearable
        );
    }

    @Test
    void testDeleteWearable() throws WearableNotFoundException {
        // given
        Wearable wearable = buildWearable();
        // when
        Mockito.when(wearableRepository.findById(wearable.getId())).thenReturn(wearable);
        wearableService.deleteWearable(wearable.getId());
        // then
        Mockito.verify(wearableRepository, times(1)).delete(wearable);
        Mockito.verify(authorizationService, atLeast(1)).validateCurrentWearableOperationAuthority(
                wearable.getId()
        );
    }

    @Test
    void testDeleteNonExistingWearable() throws WearableNotFoundException {
        // given
        Wearable wearable = buildWearable();
        // when
        Mockito.when(wearableRepository.findById(wearable.getId())).thenReturn(null); // being explicit here
        assertThrows(WearableNotFoundException.class, () -> {
            wearableService.deleteWearable(wearable.getId());
        });
        Mockito.verify(authorizationService, atLeast(1)).validateCurrentWearableOperationAuthority(
                wearable.getId()
        );
    }
}
