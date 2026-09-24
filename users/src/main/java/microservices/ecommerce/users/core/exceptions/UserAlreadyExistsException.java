package microservices.ecommerce.users.core.exceptions;

/** Registration conflicts with an existing account (username or email already in use). */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
