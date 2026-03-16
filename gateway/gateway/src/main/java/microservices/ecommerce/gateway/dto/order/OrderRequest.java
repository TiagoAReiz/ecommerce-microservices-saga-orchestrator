package microservices.ecommerce.gateway.dto.order;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        UUID userId,
        UUID shippingAddressId,
        List<OrderItemRequest> items
) {}
