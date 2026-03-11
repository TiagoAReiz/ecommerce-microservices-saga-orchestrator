package microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        UUID userId,
        UUID shippingAddressId,
        List<OrderItemRequest> items) {
}
