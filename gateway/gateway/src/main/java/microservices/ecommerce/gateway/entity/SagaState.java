package microservices.ecommerce.gateway.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("saga_states")
public class SagaState {

    @Id
    private UUID id;

    private String sagaType;
    private String currentStep;
    private String status;
    private UUID userId;
    private UUID orderId;
    private String payload;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_STARTED = "STARTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_COMPENSATING = "COMPENSATING";
    public static final String STATUS_COMPENSATED = "COMPENSATED";
}
