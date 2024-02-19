package smartfloor.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;
import smartfloor.domain.exception.FloorInaccessibleException;
import smartfloor.domain.exception.FloorNotFoundException;
import smartfloor.repository.jpa.FloorRepository;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class FloorServiceTest {
    @Mock
    private GroupService groupService;

    @Mock
    private FloorRepository floorRepository;

    @InjectMocks
    private FloorService floorService;

    /**
     * PRNG instance that we use to generate some random positions for footsteps.
     */
    private Random rand;

    @BeforeEach
    void setUp() {
        rand = new Random();
    }

    /**
     * Test obtaining a list of floors from the floor repository for a given authenticated viewer using the
     * floor-viewers table.
     */
    @Test
    void testGetFloorsByViewer() {
        // given
        User authenticatedUser = User.builder().authId("testUser").build();
        List<Floor> floors = List.of(Floor.builder().build(), Floor.builder().build());
        // id is needed because getFloors creates a Set and thus can not handle two floors with missing id as different
        Long index = 0L;
        for (Floor f : floors) f.setId(index++);

        // when
        Mockito.when(floorRepository.findByViewer(authenticatedUser)).thenReturn(floors);
        List<Floor> retrieved = floorService.getFloors(authenticatedUser); // implies using stubbed result

        // then
        assertNotNull(retrieved);
        assertEquals(floors, retrieved);
    }

    /**
     * Test obtaining a list of floors from the floor repository for a given authenticated viewer using the group-floors
     * table.
     */
    @Test
    void testGetFloorsByGroup() {
        User authenticatedUser = User.builder().authId("testUser").build();
        Group group = Group.builder()
                .tenant(Tenant.getDefaultTenant())
                .name("test")
                .managers(List.of(authenticatedUser))
                .users(List.of(authenticatedUser))
                .build();
        // given
        Floor floor1 = Floor.builder().groups(List.of(group)).build();
        floor1.setId(0L);
        List<Floor> floors = List.of(floor1);

        // when
        Mockito.when(groupService.getGroups()).thenReturn(List.of(group));
        Mockito.when(floorRepository.findByGroup(group)).thenReturn(floors);
        List<Floor> retrieved = floorService.getFloors(authenticatedUser);

        // then
        assertNotNull(retrieved);
        assertEquals(floors, retrieved);
    }

    /**
     * Test obtaining an existing floor for a given authenticated viewer.
     */
    @Test
    void testGetExistingFloor() throws FloorNotFoundException, FloorInaccessibleException {
        // given
        User authenticatedUser = User.builder().authId("testUser").build();
        Floor existingFloor = Floor.builder().viewers(List.of(authenticatedUser)).build();
        existingFloor.setId(0L);
        // when
        Mockito.when(floorRepository.findById(existingFloor.getId())).thenReturn(Optional.of(existingFloor));
        Mockito.when(floorRepository.findByViewer(authenticatedUser)).thenReturn(List.of(existingFloor));
        Floor result = floorService.getFloor(authenticatedUser, existingFloor.getId());
        // then
        assertNotNull(result);
        assertEquals(existingFloor, result);
    }

    /**
     * Test retrieving a non-existing floor from the floor repository, which should result in a FloorNotFoundException
     * being thrown.
     */
    @Test
    void testGetNonExistingFloor() throws FloorNotFoundException, FloorInaccessibleException {
        // given
        User authenticatedUser = User.builder().authId("testUser").build();
        // when
        Mockito.when(floorRepository.findById(1L)).thenReturn(Optional.empty());
        // then
        assertThrows(FloorNotFoundException.class, () -> floorService.getFloor(authenticatedUser, 1L));
    }

    /**
     * Test retrieving an existing floor from the floor repository, which has no floor-viewers and no groups
     * result in a FloorInaccessibleException being thrown.
     */
    @Test
    void testGetInaccessibleFloor() throws FloorNotFoundException, FloorInaccessibleException {
        // given
        User authenticatedUser = User.builder().authId("testUser").build();
        Long floorId = 1L;
        Floor floor = Floor.builder().viewers(new ArrayList<>()).groups(new ArrayList<>()).build();
        floor.setId(floorId);

        // when
        Mockito.when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        // then
        assertThrows(FloorInaccessibleException.class, () -> floorService.getFloor(authenticatedUser, floorId));
    }
}
