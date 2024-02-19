package smartfloor.domain.exception;

public class GroupUserLimitReachedException extends Exception {
    /**
     * TODO.
     */
    public GroupUserLimitReachedException(Integer userLimit) {
        super(String.format(
                "User limit reached for current group. The amount of users allowed for this group is %d",
                userLimit
        ));
    }
}
