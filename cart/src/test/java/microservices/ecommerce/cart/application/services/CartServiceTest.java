package microservices.ecommerce.cart.application.services;

import microservices.ecommerce.cart.application.ports.out.repositories.CartRepository;
import microservices.ecommerce.cart.core.entities.Cart;
import microservices.ecommerce.cart.core.entities.CartItem;
import microservices.ecommerce.cart.infrastructure.adapters.in.controllers.dtos.CartItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    // --- addItemToCart ---

    @Test
    void addItemToCart_noExistingCart_createsNewCartWithItem() {
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartItemRequest request = new CartItemRequest(productId, 2, BigDecimal.TEN);
        Cart result = cartService.addItemToCart(userId, request);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItemToCart_existingCart_addsNewItem() {
        Cart existingCart = buildCart(userId, "ACTIVE", new ArrayList<>());
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.of(existingCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartItemRequest request = new CartItemRequest(productId, 3, BigDecimal.valueOf(5));
        Cart result = cartService.addItemToCart(userId, request);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void addItemToCart_sameProductTwice_accumulates() {
        CartItem existingItem = new CartItem(UUID.randomUUID(), productId, 2, BigDecimal.TEN);
        Cart existingCart = buildCart(userId, "ACTIVE", new ArrayList<>(List.of(existingItem)));
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.of(existingCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartItemRequest request = new CartItemRequest(productId, 3, BigDecimal.TEN);
        Cart result = cartService.addItemToCart(userId, request);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5); // 2 + 3
    }

    // --- removeItemFromCart ---

    @Test
    void removeItemFromCart_existingItem_removesIt() {
        UUID otherProduct = UUID.randomUUID();
        CartItem item1 = new CartItem(UUID.randomUUID(), productId, 1, BigDecimal.TEN);
        CartItem item2 = new CartItem(UUID.randomUUID(), otherProduct, 2, BigDecimal.TEN);
        Cart cart = buildCart(userId, "ACTIVE", new ArrayList<>(List.of(item1, item2)));
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.removeItemFromCart(userId, productId);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductId()).isEqualTo(otherProduct);
    }

    @Test
    void removeItemFromCart_noActiveCart_throwsRuntimeException() {
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItemFromCart(userId, productId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Active cart not found");
    }

    // --- getCartByUserId ---

    @Test
    void getCartByUserId_existingActiveCart_returnsIt() {
        Cart cart = buildCart(userId, "ACTIVE", new ArrayList<>());
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.of(cart));

        Cart result = cartService.getCartByUserId(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getCartByUserId_noCart_returnsEmptyActiveCart() {
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.empty());

        Cart result = cartService.getCartByUserId(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getItems()).isEmpty();
    }

    // --- clearCart ---

    @Test
    void clearCart_existingCart_clearsItemsAndSaves() {
        CartItem item = new CartItem(UUID.randomUUID(), productId, 2, BigDecimal.TEN);
        Cart cart = buildCart(userId, "ACTIVE", new ArrayList<>(List.of(item)));
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cartService.clearCart(userId);

        verify(cartRepository).save(argThat(c -> c.getItems().isEmpty()));
    }

    @Test
    void clearCart_noActiveCart_doesNothing() {
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.empty());

        cartService.clearCart(userId);

        verify(cartRepository, never()).save(any());
    }

    // --- checkoutCart ---

    @Test
    void checkoutCart_activeCartWithItems_setsStatusToCompleted() {
        CartItem item = new CartItem(UUID.randomUUID(), productId, 1, BigDecimal.TEN);
        Cart cart = buildCart(userId, "ACTIVE", new ArrayList<>(List.of(item)));
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cartService.checkoutCart(userId);

        verify(cartRepository).save(argThat(c -> c.getStatus().equals("COMPLETED")));
    }

    @Test
    void checkoutCart_emptyCart_throwsRuntimeException() {
        Cart emptyCart = buildCart(userId, "ACTIVE", new ArrayList<>());
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> cartService.checkoutCart(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void checkoutCart_noActiveCart_throwsRuntimeException() {
        when(cartRepository.findByUserIdAndStatus(userId, "ACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.checkoutCart(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Active cart not found");
    }

    private Cart buildCart(UUID userId, String status, List<CartItem> items) {
        return new Cart(UUID.randomUUID(), userId, status,
                LocalDateTime.now(), LocalDateTime.now(), items);
    }
}
