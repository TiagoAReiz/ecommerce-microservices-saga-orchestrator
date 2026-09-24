package microservices.ecommerce.inventory.infrastructure.adapters.in.controllers.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The authenticated caller lacks the role required for the operation (HTTP 403). */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
