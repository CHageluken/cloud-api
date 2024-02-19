package smartfloor.domain.exception;

public class UserCountExceededNewLimitException extends Exception {
    public UserCountExceededNewLimitException() {
        super("The amount of users in this group is higher than the given group limit.");
    }
}
