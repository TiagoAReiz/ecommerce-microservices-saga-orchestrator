package microservices.ecommerce.users.core.exceptions;

/** Unknown username or wrong password. Deliberately does not say which one. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
