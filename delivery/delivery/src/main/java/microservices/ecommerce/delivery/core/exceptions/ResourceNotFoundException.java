package microservices.ecommerce.delivery.core.exceptions;

/**
 * The resource does not exist, or exists but belongs to another user: both answer 404 so that ids of other
 * users' resources cannot be probed.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
