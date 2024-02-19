package smartfloor.domain.exception;

public class UserMeasurementNotFoundException extends Exception {

    public UserMeasurementNotFoundException(Long id) {
        super(String.format("User measurement with id %d does not exist.", id));
    }

    public UserMeasurementNotFoundException() {
        super("User measurement does not exist.");
    }

}
