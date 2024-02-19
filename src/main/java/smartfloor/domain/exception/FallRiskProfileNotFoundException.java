package smartfloor.domain.exception;

public class FallRiskProfileNotFoundException extends Exception {
    public FallRiskProfileNotFoundException() {
        super("Fall risk profile with the provided identifier does not exist.");
    }

    public FallRiskProfileNotFoundException(Long frpId) {
        super(String.format("Fall risk profile with id %d does not exist.", frpId));
    }

    public FallRiskProfileNotFoundException(String message) {
        super(message);
    }
}
