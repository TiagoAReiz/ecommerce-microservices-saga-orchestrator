package microservices.ecommerce.cart.application.mappers;

import microservices.ecommerce.cart.core.entities.Cart;
import microservices.ecommerce.cart.core.entities.CartItem;
import microservices.ecommerce.cart.infrastructure.adapters.out.entities.CartEntity;
import microservices.ecommerce.cart.infrastructure.adapters.out.entities.CartItemEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartEntity toEntity(Cart domain) {
        if (domain == null)
            return null;
        CartEntity entity = CartEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();

        if (domain.getItems() != null) {
            List<CartItemEntity> itemEntities = domain.getItems().stream()
                    .map(itemDomain -> CartItemEntity.builder()
                            .id(itemDomain.getId())
                            .productId(itemDomain.getProductId())
                            .quantity(itemDomain.getQuantity())
                            .priceAtAddition(itemDomain.getPriceAtAddition())
                            .cart(entity)
                            .build())
                    .collect(Collectors.toList());
            entity.setItems(itemEntities);
        } else {
            entity.setItems(new ArrayList<>());
        }
        return entity;
    }

    public Cart toDomain(CartEntity entity) {
        if (entity == null)
            return null;

        List<CartItem> itemDomains = new ArrayList<>();
        if (entity.getItems() != null) {
            itemDomains = entity.getItems().stream()
                    .map(itemEntity -> new CartItem(
                            itemEntity.getId(),
                            itemEntity.getProductId(),
                            itemEntity.getQuantity(),
                            itemEntity.getPriceAtAddition()))
                    .collect(Collectors.toList());
        }

        return new Cart(
                entity.getId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                itemDomains);
    }
}
