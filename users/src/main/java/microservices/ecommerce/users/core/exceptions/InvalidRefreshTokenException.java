package microservices.ecommerce.users.core.exceptions;

/**
 * The refresh token is unknown, expired, revoked or was already used. The message is the same in every
 * case so the endpoint does not reveal which one.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
