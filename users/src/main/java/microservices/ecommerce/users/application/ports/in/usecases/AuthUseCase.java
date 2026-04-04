package microservices.ecommerce.users.application.ports.in.usecases;

import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.AuthResponse;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.LoginRequest;
import microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos.RegisterRequest;

public interface AuthUseCase {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
