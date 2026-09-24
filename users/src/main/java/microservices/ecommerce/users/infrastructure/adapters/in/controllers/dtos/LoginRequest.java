package microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
