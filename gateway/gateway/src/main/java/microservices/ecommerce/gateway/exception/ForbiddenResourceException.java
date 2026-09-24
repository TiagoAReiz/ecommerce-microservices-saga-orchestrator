package microservices.ecommerce.gateway.exception;

/** The authenticated user tried to act on a resource that belongs to someone else. */
public class ForbiddenResourceException extends RuntimeException {

    public ForbiddenResourceException(String message) {
        super(message);
    }
}
