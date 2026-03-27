package microservices.ecommerce.order.application.services;

import microservices.ecommerce.order.application.ports.out.repositories.OrderRepository;
import microservices.ecommerce.order.core.entities.Order;
import microservices.ecommerce.order.core.entities.OrderItem;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderItemRequest;
import microservices.ecommerce.order.infrastructure.adapters.in.controllers.dtos.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID orderId;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        addressId = UUID.randomUUID();
    }

    @Test
    void createOrder_validRequest_savesAndReturnsOrder() {
        UUID productId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(userId, addressId,
                List.of(new OrderItemRequest(productId, 2)));

        Order saved = buildOrder(orderId, userId, "CREATED");
        when(orderRepository.save(any())).thenReturn(saved);

        Order result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CREATED");
        assertThat(result.getUserId()).isEqualTo(userId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order toSave = captor.getValue();
        assertThat(toSave.getUserId()).isEqualTo(userId);
        assertThat(toSave.getShippingAddressId()).isEqualTo(addressId);
        assertThat(toSave.getItems()).hasSize(1);
    }

    @Test
    void createOrder_emptyItems_savesOrderWithNoItems() {
        OrderRequest request = new OrderRequest(userId, addressId, List.of());
        Order saved = buildOrder(orderId, userId, "CREATED");
        when(orderRepository.save(any())).thenReturn(saved);

        Order result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        verify(orderRepository).save(any());
    }

    @Test
    void getOrderById_existingId_returnsOrder() {
        Order order = buildOrder(orderId, userId, "CREATED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(orderId);

        assertThat(result.getId()).isEqualTo(orderId);
        verify(orderRepository).findById(orderId);
    }

    @Test
    void getOrderById_nonExistingId_throwsRuntimeException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void getOrdersByUserId_returnsAllOrders() {
        List<Order> orders = List.of(
                buildOrder(UUID.randomUUID(), userId, "CREATED"),
                buildOrder(UUID.randomUUID(), userId, "SHIPPED")
        );
        when(orderRepository.findByUserId(userId)).thenReturn(orders);

        List<Order> result = orderService.getOrdersByUserId(userId);

        assertThat(result).hasSize(2);
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    void updateOrderStatus_existingOrder_updatesStatusAndSaves() {
        Order order = buildOrder(orderId, userId, "CREATED");
        Order updated = buildOrder(orderId, userId, "SHIPPED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(updated);

        Order result = orderService.updateOrderStatus(orderId, "SHIPPED");

        assertThat(result.getStatus()).isEqualTo("SHIPPED");
        verify(orderRepository).save(argThat(o -> o.getStatus().equals("SHIPPED")));
    }

    @Test
    void updateOrderStatus_nonExistingOrder_throwsRuntimeException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "SHIPPED"))
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any());
    }

    private Order buildOrder(UUID id, UUID userId, String status) {
        return new Order(id, userId, status, BigDecimal.ZERO, addressId,
                LocalDateTime.now(), LocalDateTime.now(), List.of());
    }
}
