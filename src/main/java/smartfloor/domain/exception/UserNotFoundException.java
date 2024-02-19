package smartfloor.domain.exception;

public class UserNotFoundException extends Exception {
    public UserNotFoundException() {
        super("User with the provided identifier does not exist.");
    }

    public UserNotFoundException(Long userId) {
        super(String.format("User with id %d does not exist.", userId));
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
