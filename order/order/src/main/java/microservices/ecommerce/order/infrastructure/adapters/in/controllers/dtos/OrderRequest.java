package microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotNull UUID userId,
        @NotNull UUID shippingAddressId,
        @NotNull @NotEmpty List<@Valid OrderItemRequest> items) {
}
