package smartfloor.domain.exception;

public class FootstepNotFoundException extends Exception {

    public FootstepNotFoundException(Long footstepId) {
        super(String.format("Footstep with id %d does not exist.", footstepId));
    }

    public FootstepNotFoundException(String message) {
        super(message);
    }
}
