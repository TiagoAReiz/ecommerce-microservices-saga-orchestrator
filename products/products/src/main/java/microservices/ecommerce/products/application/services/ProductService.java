package microservices.ecommerce.products.application.services;

import lombok.RequiredArgsConstructor;
import microservices.ecommerce.products.application.ports.in.usecases.ProductUseCase;
import microservices.ecommerce.products.application.ports.out.repositories.ProductRepository;
import microservices.ecommerce.products.core.entities.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService implements ProductUseCase {

    private final ProductRepository productRepository;

    @Override
    public Product createProduct(Product product) {
        // Here we could add business validation
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id)); // Replace with a custom
                                                                                              // exception later
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product updateProduct(UUID id, Product product) {
        Product existingProduct = getProductById(id);

        // Update fields (excluding ID and CreatedAt)
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setSku(product.getSku());
        existingProduct.setCategoryId(product.getCategoryId());
        existingProduct.setActive(product.isActive());

        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }
}
