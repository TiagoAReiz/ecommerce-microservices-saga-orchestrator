package microservices.ecommerce.users.application.ports.in.usecases;

import java.util.List;
import java.util.UUID;

public record AuthResult(String token, UUID userId, String username, List<String> roles) {}
