package microservices.ecommerce.gateway.exception;

public class CompensationFailedException extends RuntimeException {

    private final String stepName;

    public CompensationFailedException(String stepName, String message, Throwable cause) {
        super(String.format("Compensation failed at step [%s]: %s", stepName, message), cause);
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }
}
