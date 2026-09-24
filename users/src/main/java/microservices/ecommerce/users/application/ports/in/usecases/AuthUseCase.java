package microservices.ecommerce.users.application.ports.in.usecases;

public interface AuthUseCase {

    /** @throws microservices.ecommerce.users.core.exceptions.UserAlreadyExistsException username or email taken */
    AuthResult register(RegisterCommand command);

    /** @throws microservices.ecommerce.users.core.exceptions.InvalidCredentialsException unknown user or wrong password */
    AuthResult login(LoginCommand command);
}
