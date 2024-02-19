package smartfloor.repository.jpa;


import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.entities.Floor;

class FloorRepositoryIntegrationTest extends IntegrationTestBase {

    private static final String TEST_FLOOR_ID1 = "newTestFloor 1";
    private static final String TEST_FLOOR_ID2 = "newTestFloor 2";
    private static final String TEST_FLOOR_ID3 = "newTestFloor 3";

    @Autowired
    FloorRepository floorRepository;


    @Test
    void testSaveOnlyFloor() {
        List<Floor> floors = floorRepository.findAll();
        int floorEntries = floors.size();

        Floor floor = new Floor();

        floor.setName(TEST_FLOOR_ID1);

        floorRepository.save(floor);
        floors = floorRepository.findAll();

        assertEquals(floorEntries + 1, floors.size(), () -> "Saving one floor adds 1 to repository size");
    }

    @Test
    void testSaveAndDeleteFloor() {
        List<Floor> floors = floorRepository.findAll();
        int floorEntries = floors.size();

        Floor floor = new Floor();
        floor.setName(TEST_FLOOR_ID2);

        floor = floorRepository.save(floor);
        floors = floorRepository.findAll();

        assertEquals(floorEntries + 1, floors.size(), () -> "Saving one floor adds 1 to repository size");

        floorRepository.delete(floor);
        floors = floorRepository.findAll();

        assertEquals(floorEntries, floors.size(), () -> "Deleting one floor subtracts 1 from repository size");
    }

    @Test
    void testFindFloor() {

        Floor floor = new Floor();
        floor.setName(TEST_FLOOR_ID3);
        floor = floorRepository.save(floor);

        Optional<Floor> tempFloor = floorRepository.findById(floor.getId());

        if (tempFloor == null) {
            assertNotNull(tempFloor, "Stored floor can not be retrieved from database");
        } else {
            assertEquals(tempFloor.get().getName(), floor.getName());
        }
    }
}
