package smartfloor.domain.exception;

public class InterventionNotFoundException extends Exception {
    public InterventionNotFoundException() {
        super("Intervention with the provided identifier does not exist.");
    }

    public InterventionNotFoundException(Long interventionId) {
        super(String.format("Intervention with id %d does not exist.", interventionId));
    }
}
