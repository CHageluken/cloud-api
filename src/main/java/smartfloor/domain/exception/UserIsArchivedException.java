package smartfloor.domain.exception;

public class UserIsArchivedException extends Exception {
    public UserIsArchivedException() {
        super("Operations for an archived user are not allowed.");
    }
}
