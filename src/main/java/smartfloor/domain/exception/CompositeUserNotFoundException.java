package smartfloor.domain.exception;

public class CompositeUserNotFoundException extends Exception {
    public CompositeUserNotFoundException() {
        super("Composite user with the provided identifier does not exist.");
    }

    public CompositeUserNotFoundException(Long compositeUserId) {
        super(String.format("Composite user with id %d does not exist.", compositeUserId));
    }

    public CompositeUserNotFoundException(String message) {
        super(message);
    }
}
