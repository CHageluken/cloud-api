package smartfloor.domain.exception;

import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.User;

public class FloorInaccessibleException extends Exception {
    public FloorInaccessibleException() {
        super("Floor with the provided identifier is not accessible for the authenticated user.");
    }

    public FloorInaccessibleException(Floor floor, User user) {
        super(String.format("Floor with id %d is not accessible to user with id %d.", floor.getId(), user.getId()));
    }

    public FloorInaccessibleException(String message) {
        super(message);
    }
}
