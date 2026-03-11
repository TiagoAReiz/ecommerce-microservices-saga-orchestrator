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

    public Product toDomain(
            microservices.ecommerce.products.infrastructure.adapters.in.controllers.dtos.ProductRequest request) {
        if (request == null)
            return null;
        return new Product(
                java.util.UUID.randomUUID(),
                request.name(),
                request.description(),
                request.price(),
                request.sku(),
                request.categoryId(),
                request.active(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());
    }

    public microservices.ecommerce.products.infrastructure.adapters.in.controllers.dtos.ProductResponse toResponse(
            Product domain) {
        if (domain == null)
            return null;
        return new microservices.ecommerce.products.infrastructure.adapters.in.controllers.dtos.ProductResponse(
                domain.getId(),
                domain.getName(),
                domain.getDescription(),
                domain.getPrice(),
                domain.getSku(),
                domain.getCategoryId(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
