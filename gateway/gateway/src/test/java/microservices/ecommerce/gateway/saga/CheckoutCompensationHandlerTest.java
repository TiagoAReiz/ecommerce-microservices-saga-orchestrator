package microservices.ecommerce.gateway.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import microservices.ecommerce.gateway.dto.delivery.DeliveryResponse;
import microservices.ecommerce.gateway.dto.inventory.InventoryResponse;
import microservices.ecommerce.gateway.entity.SagaState;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

class CheckoutCompensationHandlerTest {

    private MockWebServer mockWebServer;
    private CheckoutCompensationHandler handler;
    private ObjectMapper objectMapper;

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

        handler = new CheckoutCompensationHandler(client, client, client, client, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void compensate_fromReserveInventory_releasesInventory() throws Exception {
        UUID productId = UUID.randomUUID();
        InventoryResponse released = new InventoryResponse(UUID.randomUUID(), productId, 10, 0, null);

        String payload = objectMapper.writeValueAsString(
                java.util.List.of(new microservices.ecommerce.gateway.dto.cart.CartItemResponse(
                        UUID.randomUUID(), productId, 2, BigDecimal.TEN)));

        enqueue(released); // release inventory

        SagaState saga = buildSaga("RESERVE_INVENTORY", null, payload);

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        // Verify one request was made (the inventory release)
        assert mockWebServer.getRequestCount() == 1;
    }

    @Test
    void compensate_fromCreateOrder_cancelsOrderAndReleasesInventory() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryResponse released = new InventoryResponse(UUID.randomUUID(), productId, 10, 0, null);

        String payload = objectMapper.writeValueAsString(
                java.util.List.of(new microservices.ecommerce.gateway.dto.cart.CartItemResponse(
                        UUID.randomUUID(), productId, 1, BigDecimal.TEN)));

        mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // cancel order
        enqueue(released);                                              // release inventory

        SagaState saga = buildSaga("CREATE_ORDER", orderId, payload);

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assert mockWebServer.getRequestCount() == 2;
    }

    @Test
    void compensate_fromScheduleDelivery_fullCompensationChain() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryResponse released = new InventoryResponse(UUID.randomUUID(), productId, 10, 0, null);
        DeliveryResponse delivery = new DeliveryResponse(deliveryId, orderId, "CARRIER", "TRK",
                "PREPARING", null, null, null, null);
        DeliveryResponse cancelledDelivery = new DeliveryResponse(deliveryId, orderId, "CARRIER", "TRK",
                "CANCELLED", null, null, null, null);

        String payload = objectMapper.writeValueAsString(
                java.util.List.of(new microservices.ecommerce.gateway.dto.cart.CartItemResponse(
                        UUID.randomUUID(), productId, 1, BigDecimal.TEN)));

        enqueue(delivery);                                              // GET delivery
        enqueue(cancelledDelivery);                                     // PATCH delivery status
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // no payment found
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // cancel order
        enqueue(released);                                              // release inventory

        SagaState saga = buildSaga("SCHEDULE_DELIVERY", orderId, payload);

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();
    }

    @Test
    void compensate_emptyPayload_skipsInventoryRelease() {
        UUID orderId = UUID.randomUUID();

        mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // cancel order (step = CREATE_ORDER)

        SagaState saga = buildSaga("CREATE_ORDER", orderId, "[]");

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assert mockWebServer.getRequestCount() == 1; // only cancel order, no inventory release
    }

    @Test
    void compensate_inventoryReleaseFails_continuesGracefully() throws Exception {
        UUID productId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(
                java.util.List.of(new microservices.ecommerce.gateway.dto.cart.CartItemResponse(
                        UUID.randomUUID(), productId, 2, BigDecimal.TEN)));

        mockWebServer.enqueue(new MockResponse().setResponseCode(500)); // release inventory fails

        SagaState saga = buildSaga("RESERVE_INVENTORY", null, payload);

        // Should complete without propagating the error (best-effort)
        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();
    }

    private SagaState buildSaga(String step, UUID orderId, String payload) {
        return SagaState.builder()
                .id(UUID.randomUUID())
                .sagaType("CHECKOUT")
                .currentStep(step)
                .status(SagaState.STATUS_IN_PROGRESS)
                .orderId(orderId)
                .payload(payload)
                .build();
    }

    private void enqueue(Object body) throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(body))
                .addHeader("Content-Type", "application/json"));
    }
}
