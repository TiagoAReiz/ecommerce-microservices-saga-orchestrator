package microservices.ecommerce.users.application.ports.in.usecases;

public record RegisterCommand(String username, String email, String rawPassword) {}
