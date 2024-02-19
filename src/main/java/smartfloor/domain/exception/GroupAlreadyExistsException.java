package smartfloor.domain.exception;

public class GroupAlreadyExistsException extends Exception {
    public GroupAlreadyExistsException(String name) {
        super(String.format("A group with name %s already exists.", name));
    }
}
