package microservices.ecommerce.products.application.mappers;

import microservices.ecommerce.products.core.entities.Product;
import microservices.ecommerce.products.infrastructure.adapters.out.entities.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductEntity toEntity(Product domain) {
        if (domain == null)
            return null;
        return ProductEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .price(domain.getPrice())
                .sku(domain.getSku())
                .categoryId(domain.getCategoryId())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Product toDomain(ProductEntity entity) {
        if (entity == null)
            return null;
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getSku(),
                entity.getCategoryId(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
