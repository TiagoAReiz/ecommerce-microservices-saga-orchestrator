package microservices.ecommerce.users.application.ports.in.usecases;

public interface AuthUseCase {

    /** @throws microservices.ecommerce.users.core.exceptions.UserAlreadyExistsException username or email taken */
    AuthResult register(RegisterCommand command);

    /** @throws microservices.ecommerce.users.core.exceptions.InvalidCredentialsException unknown user or wrong password */
    AuthResult login(LoginCommand command);

    /**
     * Exchanges a refresh token for a new access token and a new refresh token (rotation). The presented
     * token is revoked; presenting it again revokes its whole family (reuse detection).
     *
     * @throws microservices.ecommerce.users.core.exceptions.InvalidRefreshTokenException unknown, expired,
     *         revoked or reused token
     */
    AuthResult refresh(String refreshToken);

    /** Revokes the refresh token's whole family (the login session). Unknown tokens are ignored. */
    void logout(String refreshToken);
}
