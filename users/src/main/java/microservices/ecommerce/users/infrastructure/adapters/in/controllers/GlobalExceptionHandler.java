package microservices.ecommerce.users.infrastructure.adapters.in.controllers;

import microservices.ecommerce.users.core.exceptions.InvalidCredentialsException;
import microservices.ecommerce.users.core.exceptions.UserAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        return Map.of("status", 400, "error", "Validation failed", "fields", fieldErrors);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleInvalidCredentials(InvalidCredentialsException ex) {
        return Map.of("status", 401, "error", "Unauthorized", "message", ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return Map.of("status", 409, "error", "Conflict", "message", ex.getMessage());
    }

    /** Two concurrent registrations can pass the existence checks; the unique constraints still reject one. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        return Map.of("status", 409, "error", "Conflict", "message", "Username or email already registered");
    }
}
