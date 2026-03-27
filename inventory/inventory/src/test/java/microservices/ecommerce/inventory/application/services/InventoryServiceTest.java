package microservices.ecommerce.inventory.application.services;

import microservices.ecommerce.inventory.application.ports.out.repositories.InventoryRepository;
import microservices.ecommerce.inventory.core.entities.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    // --- addStock ---

    @Test
    void addStock_newProduct_createsInventoryWithQuantity() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Inventory result = inventoryService.addStock(productId, 10);

        assertThat(result.getQuantityAvailable()).isEqualTo(10);
        assertThat(result.getProductId()).isEqualTo(productId);
    }

    @Test
    void addStock_existingProduct_addsToCurrentQuantity() {
        Inventory existing = new Inventory(UUID.randomUUID(), productId, 5, 0, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Inventory result = inventoryService.addStock(productId, 10);

        assertThat(result.getQuantityAvailable()).isEqualTo(15);
    }

    @Test
    void addStock_zeroQuantity_throwsIllegalArgument() {
        assertThatThrownBy(() -> inventoryService.addStock(productId, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void addStock_negativeQuantity_throwsIllegalArgument() {
        assertThatThrownBy(() -> inventoryService.addStock(productId, -5))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(inventoryRepository);
    }

    // --- reserveStock ---

    @Test
    void reserveStock_sufficientStock_movesAvailableToReserved() {
        Inventory existing = new Inventory(UUID.randomUUID(), productId, 10, 0, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Inventory result = inventoryService.reserveStock(productId, 3);

        assertThat(result.getQuantityAvailable()).isEqualTo(7);
        assertThat(result.getQuantityReserved()).isEqualTo(3);
    }

    @Test
    void reserveStock_insufficientStock_throwsRuntimeException() {
        Inventory existing = new Inventory(UUID.randomUUID(), productId, 2, 0, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> inventoryService.reserveStock(productId, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserveStock_notFound_throwsRuntimeException() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserveStock(productId, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found");
    }

    @Test
    void reserveStock_zeroQuantity_throwsIllegalArgument() {
        assertThatThrownBy(() -> inventoryService.reserveStock(productId, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(inventoryRepository);
    }

    // --- releaseStock ---

    @Test
    void releaseStock_validReserved_movesReservedBackToAvailable() {
        Inventory existing = new Inventory(UUID.randomUUID(), productId, 5, 3, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Inventory result = inventoryService.releaseStock(productId, 3);

        assertThat(result.getQuantityAvailable()).isEqualTo(8);
        assertThat(result.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    void releaseStock_moreThanReserved_throwsRuntimeException() {
        Inventory existing = new Inventory(UUID.randomUUID(), productId, 5, 1, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> inventoryService.releaseStock(productId, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot release more stock");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void releaseStock_notFound_throwsRuntimeException() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.releaseStock(productId, 1))
                .isInstanceOf(RuntimeException.class);
    }

    // --- getInventoryByProductId ---

    @Test
    void getInventoryByProductId_existing_returnsInventory() {
        Inventory existing = new Inventory(UUID.randomUUID(), productId, 10, 2, LocalDateTime.now());
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));

        Inventory result = inventoryService.getInventoryByProductId(productId);

        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getQuantityAvailable()).isEqualTo(10);
    }

    @Test
    void getInventoryByProductId_notFound_returnsEmptyInventory() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        Inventory result = inventoryService.getInventoryByProductId(productId);

        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getQuantityAvailable()).isEqualTo(0);
    }
}
