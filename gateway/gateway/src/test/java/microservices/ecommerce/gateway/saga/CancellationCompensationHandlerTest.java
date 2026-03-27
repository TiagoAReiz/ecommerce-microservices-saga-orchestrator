package microservices.ecommerce.gateway.saga;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import microservices.ecommerce.gateway.entity.SagaState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationCompensationHandlerTest {

    private MockWebServer mockWebServer;
    private CancellationCompensationHandler handler;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient orderClient = WebClient.create(mockWebServer.url("/").toString());
        handler = new CancellationCompensationHandler(orderClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void compensate_fromGetOrder_completesWithoutHttpCalls() {
        SagaState saga = buildSaga("GET_ORDER", UUID.randomUUID());

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void compensate_fromInitiated_completesWithoutHttpCalls() {
        SagaState saga = buildSaga("INITIATED", null);

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void compensate_fromCancelOrder_revertsOrderToPending() {
        UUID orderId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        SagaState saga = buildSaga("CANCEL_ORDER", orderId);

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void compensate_fromCancelOrder_revertFails_completesGracefully() {
        UUID orderId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        SagaState saga = buildSaga("CANCEL_ORDER", orderId);

        // Should complete without propagating the error (best-effort)
        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();
    }

    @Test
    void compensate_fromCancelOrder_nullOrderId_completesWithoutHttpCalls() {
        SagaState saga = buildSaga("CANCEL_ORDER", null);

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void compensate_fromReleaseInventory_irreversible_completesWithoutHttpCalls() {
        SagaState saga = buildSaga("RELEASE_INVENTORY", UUID.randomUUID());

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void compensate_fromRefundPayment_irreversible_completesWithoutHttpCalls() {
        SagaState saga = buildSaga("REFUND_PAYMENT", UUID.randomUUID());

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void compensate_fromCancelDelivery_irreversible_completesWithoutHttpCalls() {
        SagaState saga = buildSaga("CANCEL_DELIVERY", UUID.randomUUID());

        StepVerifier.create(handler.compensate(saga))
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    private SagaState buildSaga(String step, UUID orderId) {
        return SagaState.builder()
                .id(UUID.randomUUID())
                .sagaType("ORDER_CANCELLATION")
                .currentStep(step)
                .status(SagaState.STATUS_IN_PROGRESS)
                .orderId(orderId)
                .build();
    }
}
