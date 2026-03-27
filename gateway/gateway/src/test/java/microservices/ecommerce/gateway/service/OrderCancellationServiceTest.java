package microservices.ecommerce.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import microservices.ecommerce.gateway.dto.delivery.DeliveryResponse;
import microservices.ecommerce.gateway.dto.inventory.InventoryResponse;
import microservices.ecommerce.gateway.dto.order.OrderItemResponse;
import microservices.ecommerce.gateway.dto.order.OrderResponse;
import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.saga.SagaExecutionCoordinator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    private MockWebServer mockWebServer;
    private OrderCancellationService cancellationService;
    private ObjectMapper objectMapper;

    @Mock
    private SagaExecutionCoordinator sagaCoordinator;

    private SagaState saga;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String baseUrl = mockWebServer.url("/").toString();
        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs()
                        .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper)))
                .build();

        saga = SagaState.builder()
                .id(UUID.randomUUID())
                .sagaType("ORDER_CANCELLATION")
                .status(SagaState.STATUS_STARTED)
                .build();

        when(sagaCoordinator.startSaga(anyString(), any())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.setOrderId(any(), any())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.updateStep(any(), anyString())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.completeSaga(any())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.failSaga(any(), anyString())).thenReturn(Mono.just(saga));

        cancellationService = new OrderCancellationService(client, client, client, client, sagaCoordinator);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void cancelOrder_happyPath_returnsResponse() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        OrderItemResponse orderItem = new OrderItemResponse(UUID.randomUUID(), productId, 2, BigDecimal.TEN);
        OrderResponse order = new OrderResponse(orderId, UUID.randomUUID(), "CREATED", BigDecimal.valueOf(20),
                UUID.randomUUID(), null, null, List.of(orderItem));
        OrderResponse cancelled = new OrderResponse(orderId, order.userId(), "CANCELLED", order.totalAmount(),
                order.shippingAddressId(), null, null, List.of(orderItem));
        InventoryResponse inventoryResp = new InventoryResponse(UUID.randomUUID(), productId, 10, 0, null);
        DeliveryResponse delivery = new DeliveryResponse(deliveryId, orderId, "CARRIER", "TRK",
                "PREPARING", null, null, null, null);
        DeliveryResponse cancelledDelivery = new DeliveryResponse(deliveryId, orderId, "CARRIER", "TRK",
                "CANCELLED", null, null, null, null);

        enqueue(order);                    // GET /api/v1/orders/{id}
        enqueue(cancelled);                // PATCH /api/v1/orders/{id}/status (CANCELLED)
        enqueue(inventoryResp);            // POST /api/v1/inventory/release
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // GET /api/v1/payments/order (no payment)
        enqueue(delivery);                 // GET /api/v1/deliveries/order/{id}
        enqueue(cancelledDelivery);        // PATCH /api/v1/deliveries/{id}/status (CANCELLED)

        StepVerifier.create(cancellationService.cancelOrder(orderId))
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(orderId);
                    assertThat(response.status()).isEqualTo("CANCELLED");
                })
                .verifyComplete();

        verify(sagaCoordinator).completeSaga(any());
    }

    @Test
    void cancelOrder_shippedOrder_failsWithIllegalState() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse shippedOrder = new OrderResponse(orderId, UUID.randomUUID(), "SHIPPED",
                BigDecimal.TEN, UUID.randomUUID(), null, null, List.of());
        enqueue(shippedOrder);

        StepVerifier.create(cancellationService.cancelOrder(orderId))
                .expectError(IllegalStateException.class)
                .verify();

        verify(sagaCoordinator).failSaga(any(), anyString());
    }

    @Test
    void cancelOrder_deliveredOrder_failsWithIllegalState() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse deliveredOrder = new OrderResponse(orderId, UUID.randomUUID(), "DELIVERED",
                BigDecimal.TEN, UUID.randomUUID(), null, null, List.of());
        enqueue(deliveredOrder);

        StepVerifier.create(cancellationService.cancelOrder(orderId))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void cancelOrder_noDelivery_stillCompletes() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemResponse item = new OrderItemResponse(UUID.randomUUID(), productId, 1, BigDecimal.TEN);
        OrderResponse order = new OrderResponse(orderId, UUID.randomUUID(), "CREATED",
                BigDecimal.TEN, UUID.randomUUID(), null, null, List.of(item));
        OrderResponse cancelled = new OrderResponse(orderId, order.userId(), "CANCELLED",
                order.totalAmount(), order.shippingAddressId(), null, null, List.of(item));
        InventoryResponse inventoryResp = new InventoryResponse(UUID.randomUUID(), productId, 10, 0, null);

        enqueue(order);
        enqueue(cancelled);
        enqueue(inventoryResp);            // release inventory
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // no payment found
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // no delivery found

        StepVerifier.create(cancellationService.cancelOrder(orderId))
                .assertNext(response -> assertThat(response.status()).isEqualTo("CANCELLED"))
                .verifyComplete();
    }

    private void enqueue(Object body) throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(body))
                .addHeader("Content-Type", "application/json"));
    }
}
