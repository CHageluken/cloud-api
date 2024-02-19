package smartfloor.domain.exception;

public class WearableAlreadyExistsException extends Exception {
    public WearableAlreadyExistsException(String wearableId) {
        super("Wearable with id " + wearableId + " already exists.");
    }
}
