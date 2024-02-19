package smartfloor.domain.exception;

/**
 * Created by tim on 9/23/16.
 */
public class WearableNotFoundException extends Exception {
    public WearableNotFoundException(String wearableId) {
        super(String.format("Wearable with id %s does not exist.", wearableId));
    }
}
