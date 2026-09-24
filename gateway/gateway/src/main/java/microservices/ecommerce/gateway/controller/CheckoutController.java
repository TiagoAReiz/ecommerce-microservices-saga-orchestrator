package microservices.ecommerce.gateway.controller;

import microservices.ecommerce.gateway.dto.checkout.CheckoutRequest;
import microservices.ecommerce.gateway.dto.checkout.CheckoutResponse;
import microservices.ecommerce.gateway.filter.JwtAuthenticationFilter;
import microservices.ecommerce.gateway.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    public Mono<ResponseEntity<CheckoutResponse>> checkout(
            @RequestHeader(JwtAuthenticationFilter.USER_ID_HEADER) UUID userId,
            @Valid @RequestBody CheckoutRequest request) {
        log.info("Checkout request received for user: {}", userId);
        return checkoutService.executeCheckout(userId, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
