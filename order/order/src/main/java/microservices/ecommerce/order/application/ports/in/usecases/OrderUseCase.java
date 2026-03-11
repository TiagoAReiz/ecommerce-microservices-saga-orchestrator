package microservices.ecommerce.order.application.ports.in.usecases;

import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderRequest;
import java.util.List;
import java.util.UUID;

public interface OrderUseCase {

    Order createOrder(OrderRequest orderRequest);

    Order getOrderById(UUID id);

    List<Order> getOrdersByUserId(UUID userId);

    Order updateOrderStatus(UUID id, String status);
}
