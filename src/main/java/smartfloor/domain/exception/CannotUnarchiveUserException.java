package smartfloor.domain.exception;

public class CannotUnarchiveUserException extends Exception {
    public CannotUnarchiveUserException() {
        super("User has been archived too soon.");
    }
}
