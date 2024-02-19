package smartfloor.domain.exception;

public class TestResultNotFoundException extends Exception {
    public TestResultNotFoundException(Long id) {
        super(String.format("Test result for id %d not found.", id));
    }
}
