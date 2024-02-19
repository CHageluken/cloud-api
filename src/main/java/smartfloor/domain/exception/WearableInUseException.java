package smartfloor.domain.exception;

public class WearableInUseException extends Exception {
    public WearableInUseException(String wearableId, Long userId) {
        super(String.format("Wearable with id %s is already in use by user with id %d.", wearableId, userId));
    }
}
