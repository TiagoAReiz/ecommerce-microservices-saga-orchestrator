package microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos;

import java.util.List;
import java.util.UUID;

public record CartRequest(
        UUID userId,
        List<CartItemRequest> items) {
}
