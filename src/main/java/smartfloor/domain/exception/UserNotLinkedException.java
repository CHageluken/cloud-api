package smartfloor.domain.exception;

public class UserNotLinkedException extends Exception {
    public UserNotLinkedException(Long userId) {
        super(String.format("User with id %d is currently not linked to any device.", userId));
    }
}
