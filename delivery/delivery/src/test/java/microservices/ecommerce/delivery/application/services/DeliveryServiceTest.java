package microservices.ecommerce.delivery.application.services;

import microservices.ecommerce.delivery.application.ports.out.repositories.DeliveryRepository;
import microservices.ecommerce.delivery.core.entities.Delivery;
import microservices.ecommerce.delivery.infrastructure.adapters.in.controllers.dtos.DeliveryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    private UUID deliveryId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        deliveryId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    void scheduleDelivery_validRequest_savesWithPreparingStatus() {
        DeliveryRequest request = new DeliveryRequest(orderId, "DHL", "TRK-001",
                LocalDateTime.now().plusDays(7));
        Delivery saved = buildDelivery(deliveryId, orderId, "PREPARING");
        when(deliveryRepository.save(any())).thenReturn(saved);

        Delivery result = deliveryService.scheduleDelivery(request);

        assertThat(result.getStatus()).isEqualTo("PREPARING");
        assertThat(result.getOrderId()).isEqualTo(orderId);

        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(captor.capture());
        Delivery toSave = captor.getValue();
        assertThat(toSave.getStatus()).isEqualTo("PREPARING");
        assertThat(toSave.getCarrier()).isEqualTo("DHL");
        assertThat(toSave.getTrackingCode()).isEqualTo("TRK-001");
        assertThat(toSave.getActualDeliveryDate()).isNull();
    }

    @Test
    void getDeliveryById_existingId_returnsDelivery() {
        Delivery delivery = buildDelivery(deliveryId, orderId, "PREPARING");
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        Delivery result = deliveryService.getDeliveryById(deliveryId);

        assertThat(result.getId()).isEqualTo(deliveryId);
    }

    @Test
    void getDeliveryById_nonExistingId_throwsRuntimeException() {
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getDeliveryById(deliveryId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(deliveryId.toString());
    }

    @Test
    void getDeliveryByOrderId_existing_returnsOptionalWithDelivery() {
        Delivery delivery = buildDelivery(deliveryId, orderId, "PREPARING");
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        Optional<Delivery> result = deliveryService.getDeliveryByOrderId(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void getDeliveryByOrderId_notFound_returnsEmptyOptional() {
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        Optional<Delivery> result = deliveryService.getDeliveryByOrderId(orderId);

        assertThat(result).isEmpty();
    }

    @Test
    void updateDeliveryStatus_normalStatus_updatesAndSaves() {
        Delivery delivery = buildDelivery(deliveryId, orderId, "PREPARING");
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Delivery result = deliveryService.updateDeliveryStatus(deliveryId, "SHIPPED");

        assertThat(result.getStatus()).isEqualTo("SHIPPED");
        assertThat(result.getActualDeliveryDate()).isNull(); // not set unless DELIVERED
    }

    @Test
    void updateDeliveryStatus_deliveredStatus_setsActualDeliveryDate() {
        Delivery delivery = buildDelivery(deliveryId, orderId, "SHIPPED");
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Delivery result = deliveryService.updateDeliveryStatus(deliveryId, "DELIVERED");

        assertThat(result.getStatus()).isEqualTo("DELIVERED");
        assertThat(result.getActualDeliveryDate()).isNotNull();
    }

    @Test
    void updateDeliveryStatus_nonExistingDelivery_throwsRuntimeException() {
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(deliveryId, "SHIPPED"))
                .isInstanceOf(RuntimeException.class);

        verify(deliveryRepository, never()).save(any());
    }

    private Delivery buildDelivery(UUID id, UUID orderId, String status) {
        return new Delivery(id, orderId, "DHL", "TRK-001", status,
                LocalDateTime.now().plusDays(7), null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
