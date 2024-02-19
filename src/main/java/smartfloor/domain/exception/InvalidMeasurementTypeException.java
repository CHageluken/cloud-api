package smartfloor.domain.exception;

public class InvalidMeasurementTypeException extends Exception {
    public InvalidMeasurementTypeException() {
        super("The provided measurement type does not exist.");
    }
}
