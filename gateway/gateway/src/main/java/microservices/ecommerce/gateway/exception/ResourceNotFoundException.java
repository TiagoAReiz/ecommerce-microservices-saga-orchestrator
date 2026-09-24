package microservices.ecommerce.gateway.exception;

/**
 * The resource does not exist, or it belongs to another user. Both cases answer 404 so that callers cannot
 * probe which ids exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
