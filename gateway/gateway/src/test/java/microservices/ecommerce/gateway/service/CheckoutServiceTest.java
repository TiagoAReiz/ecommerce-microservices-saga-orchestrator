package microservices.ecommerce.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import microservices.ecommerce.gateway.dto.cart.CartItemResponse;
import microservices.ecommerce.gateway.dto.cart.CartResponse;
import microservices.ecommerce.gateway.dto.checkout.CheckoutRequest;
import microservices.ecommerce.gateway.dto.checkout.CheckoutResponse;
import microservices.ecommerce.gateway.dto.delivery.DeliveryResponse;
import microservices.ecommerce.gateway.dto.inventory.InventoryResponse;
import microservices.ecommerce.gateway.dto.order.OrderResponse;
import microservices.ecommerce.gateway.dto.payment.PaymentResponse;
import microservices.ecommerce.gateway.entity.SagaState;
import microservices.ecommerce.gateway.exception.SagaStepFailedException;
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
class CheckoutServiceTest {

    private MockWebServer mockWebServer;
    private CheckoutService checkoutService;
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
                .sagaType("CHECKOUT")
                .status(SagaState.STATUS_STARTED)
                .build();

        when(sagaCoordinator.startSaga(anyString(), any())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.updateStep(any(), anyString())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.savePayload(any(), anyString())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.setOrderId(any(), any())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.completeSaga(any())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.failSaga(any(), anyString())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.startCompensation(any())).thenReturn(Mono.just(saga));
        when(sagaCoordinator.completeCompensation(any())).thenReturn(Mono.just(saga));

        checkoutService = new CheckoutService(client, client, client, client, client,
                sagaCoordinator, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void executeCheckout_happyPath_returnsResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        CartItemResponse item = new CartItemResponse(UUID.randomUUID(), productId, 2, BigDecimal.TEN);
        CartResponse cart = new CartResponse(UUID.randomUUID(), userId, "ACTIVE", null, null, List.of(item));
        InventoryResponse inventory = new InventoryResponse(UUID.randomUUID(), productId, 8, 2, null);
        OrderResponse order = new OrderResponse(orderId, userId, "CREATED", BigDecimal.valueOf(20),
                addressId, null, null, List.of());
        PaymentResponse payment = new PaymentResponse(UUID.randomUUID(), orderId, BigDecimal.valueOf(20),
                "BRL", "AUTHORIZED", "CREDIT_CARD", "TXN-ABC123", null, null);
        DeliveryResponse delivery = new DeliveryResponse(UUID.randomUUID(), orderId, "CARRIER",
                "TRK-001", "PREPARING", null, null, null, null);

        enqueue(cart);
        enqueue(inventory);
        enqueue(order);
        enqueue(payment);
        enqueue(delivery);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // checkout cart

        CheckoutRequest request = new CheckoutRequest(userId, addressId, "BRL", "CREDIT_CARD");

        StepVerifier.create(checkoutService.executeCheckout(request))
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo(orderId);
                    assertThat(response.paymentStatus()).isEqualTo("AUTHORIZED");
                    assertThat(response.transactionReference()).isEqualTo("TXN-ABC123");
                })
                .verifyComplete();

        verify(sagaCoordinator).completeSaga(any());
    }

    @Test
    void executeCheckout_emptyCart_failsWithIllegalArgument() throws Exception {
        UUID userId = UUID.randomUUID();
        CartResponse emptyCart = new CartResponse(UUID.randomUUID(), userId, "ACTIVE", null, null, List.of());
        enqueue(emptyCart);

        CheckoutRequest request = new CheckoutRequest(userId, UUID.randomUUID(), "BRL", "CREDIT_CARD");

        StepVerifier.create(checkoutService.executeCheckout(request))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(sagaCoordinator).failSaga(any(), anyString());
    }

    @Test
    void executeCheckout_inventoryFails_throwsSagaStepFailedException() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CartItemResponse item = new CartItemResponse(UUID.randomUUID(), productId, 5, BigDecimal.TEN);
        CartResponse cart = new CartResponse(UUID.randomUUID(), userId, "ACTIVE", null, null, List.of(item));
        enqueue(cart);
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Inventory error"));

        CheckoutRequest request = new CheckoutRequest(userId, UUID.randomUUID(), "BRL", "CREDIT_CARD");

        StepVerifier.create(checkoutService.executeCheckout(request))
                .expectError(SagaStepFailedException.class)
                .verify();
    }

    @Test
    void executeCheckout_paymentFails_triggersCompensation() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        CartItemResponse item = new CartItemResponse(UUID.randomUUID(), productId, 1, BigDecimal.TEN);
        CartResponse cart = new CartResponse(UUID.randomUUID(), userId, "ACTIVE", null, null, List.of(item));
        InventoryResponse inventory = new InventoryResponse(UUID.randomUUID(), productId, 9, 1, null);
        OrderResponse order = new OrderResponse(orderId, userId, "CREATED", BigDecimal.TEN,
                addressId, null, null, List.of());

        enqueue(cart);
        enqueue(inventory);
        enqueue(order);
        mockWebServer.enqueue(new MockResponse().setResponseCode(500)); // payment fails
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // cancel order (compensation)
        enqueue(inventory);                                              // release inventory (compensation)

        CheckoutRequest request = new CheckoutRequest(userId, addressId, "BRL", "CREDIT_CARD");

        StepVerifier.create(checkoutService.executeCheckout(request))
                .expectError(SagaStepFailedException.class)
                .verify();

        verify(sagaCoordinator).startCompensation(any());
        verify(sagaCoordinator).completeCompensation(any());
    }

    private void enqueue(Object body) throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(body))
                .addHeader("Content-Type", "application/json"));
    }
}
