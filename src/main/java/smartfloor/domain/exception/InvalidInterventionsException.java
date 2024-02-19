package smartfloor.domain.exception;

public class InvalidInterventionsException extends Exception {
    public InvalidInterventionsException() {
        super("Invalid request body for interventions creation.");
    }
}
