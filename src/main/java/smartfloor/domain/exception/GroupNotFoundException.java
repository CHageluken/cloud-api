package smartfloor.domain.exception;

public class GroupNotFoundException extends Exception {
    public static final String GROUP_NOT_FOUND_RESPONSE_MESSAGE_DEFAULT =
            "Group with the provided identifier does not exist.";
    public static final String GROUP_NOT_FOUND_RESPONSE_MESSAGE_ID = "Group with id %d not found.";
    public static final String GROUP_NOT_FOUND_RESPONSE_MESSAGE_NAME = "Group with name %s not found.";

    public GroupNotFoundException() {
        super(GROUP_NOT_FOUND_RESPONSE_MESSAGE_DEFAULT);
    }

    public GroupNotFoundException(Long id) {
        super(String.format(GROUP_NOT_FOUND_RESPONSE_MESSAGE_ID, id));
    }

    public GroupNotFoundException(String name) {
        super(String.format(GROUP_NOT_FOUND_RESPONSE_MESSAGE_NAME, name));
    }
}
