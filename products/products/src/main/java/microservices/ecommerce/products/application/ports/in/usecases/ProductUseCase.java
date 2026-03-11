package microservices.ecommerce.products.application.ports.in.usecases;

import microservices.ecommerce.products.core.entities.Product;
import java.util.List;
import java.util.UUID;

public interface ProductUseCase {

    Product createProduct(Product product);

    Product getProductById(UUID id);

    List<Product> getAllProducts();

    Product updateProduct(UUID id, Product product);

    void deleteProduct(UUID id);
}
