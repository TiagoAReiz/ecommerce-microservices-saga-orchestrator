package microservices.ecommerce.gateway.exception;

public class SagaStepFailedException extends RuntimeException {

    private final String stepName;
    private final String sagaType;

    public SagaStepFailedException(String sagaType, String stepName, String message) {
        super(String.format("Saga [%s] failed at step [%s]: %s", sagaType, stepName, message));
        this.sagaType = sagaType;
        this.stepName = stepName;
    }

    public SagaStepFailedException(String sagaType, String stepName, String message, Throwable cause) {
        super(String.format("Saga [%s] failed at step [%s]: %s", sagaType, stepName, message), cause);
        this.sagaType = sagaType;
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }

    public String getSagaType() {
        return sagaType;
    }
}
