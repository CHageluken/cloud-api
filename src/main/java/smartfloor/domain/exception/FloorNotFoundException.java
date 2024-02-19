package smartfloor.domain.exception;

public class FloorNotFoundException extends Exception {
    public FloorNotFoundException() {
        super("Floor with the provided identifier does not exist.");
    }

    public FloorNotFoundException(Long floorId) {
        super(String.format("Floor with id %d does not exist.", floorId));
    }

    public FloorNotFoundException(String message) {
        super(message);
    }
}
