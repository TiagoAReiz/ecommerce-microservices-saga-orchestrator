package microservices.ecommerce.users.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes of the password
        @NotBlank @Size(min = 6, max = 72) String password
) {}
