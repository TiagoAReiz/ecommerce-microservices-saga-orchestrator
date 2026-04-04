package microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username,
        List<String> roles
) {}
