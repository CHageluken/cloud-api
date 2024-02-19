package smartfloor.domain.exception;

public class WearableNotInUseException extends Exception {
    public WearableNotInUseException(String wearableId) {
        super(String.format("Wearable with id %s is currently not in use (actively linked to a user).", wearableId));
    }
}
