package microservices.ecommerce.gateway.controller;

import microservices.ecommerce.gateway.dto.cancellation.CancellationResponse;
import microservices.ecommerce.gateway.dto.checkout.CheckoutRequest;
import microservices.ecommerce.gateway.dto.checkout.CheckoutResponse;
import microservices.ecommerce.gateway.filter.JwtAuthenticationFilter;
import microservices.ecommerce.gateway.service.CheckoutService;
import microservices.ecommerce.gateway.service.OrderCancellationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The orchestration endpoints must act as the authenticated user (the {@code X-User-Id} header set by
 * {@link JwtAuthenticationFilter} from the verified JWT), never as a user id chosen by the client.
 */
class UserIdentityBindingTest {

    private CheckoutService checkoutService;
    private OrderCancellationService cancellationService;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        checkoutService = mock(CheckoutService.class);
        cancellationService = mock(OrderCancellationService.class);
        client = WebTestClient.bindToController(
                        new CheckoutController(checkoutService),
                        new OrderCancellationController(cancellationService))
                .build();
    }

    @Test
    void checkout_usesAuthenticatedUserId_andIgnoresUserIdInBody() {
        UUID authenticatedUser = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(checkoutService.executeCheckout(any(), any())).thenReturn(Mono.just(
                new CheckoutResponse(UUID.randomUUID(), "CREATED", "AUTHORIZED", "TXN", UUID.randomUUID())));

        client.post().uri("/api/v1/checkout")
                .header(JwtAuthenticationFilter.USER_ID_HEADER, authenticatedUser.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId":"%s","shippingAddressId":"%s","currency":"BRL","paymentMethod":"CREDIT_CARD"}
                        """.formatted(otherUser, addressId))
                .exchange()
                .expectStatus().isCreated();

        ArgumentCaptor<CheckoutRequest> request = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(checkoutService).executeCheckout(eq(authenticatedUser), request.capture());
        assertThat(request.getValue().shippingAddressId()).isEqualTo(addressId);
    }

    @Test
    void checkout_withoutAuthenticatedUser_isRejected() {
        client.post().uri("/api/v1/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"shippingAddressId":"%s","currency":"BRL","paymentMethod":"CREDIT_CARD"}
                        """.formatted(UUID.randomUUID()))
                .exchange()
                .expectStatus().isBadRequest();

        verify(checkoutService, never()).executeCheckout(any(), any());
    }

    @Test
    void cancelOrder_passesAuthenticatedUserIdForOwnershipCheck() {
        UUID authenticatedUser = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(cancellationService.cancelOrder(orderId, authenticatedUser))
                .thenReturn(Mono.just(new CancellationResponse(orderId, "CANCELLED", "ok")));

        client.post().uri("/api/v1/orders/{id}/cancel", orderId)
                .header(JwtAuthenticationFilter.USER_ID_HEADER, authenticatedUser.toString())
                .exchange()
                .expectStatus().isOk();

        verify(cancellationService).cancelOrder(orderId, authenticatedUser);
    }
}
