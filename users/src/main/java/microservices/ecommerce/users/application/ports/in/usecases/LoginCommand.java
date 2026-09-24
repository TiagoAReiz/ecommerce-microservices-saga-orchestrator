package microservices.ecommerce.users.application.ports.in.usecases;

public record LoginCommand(String username, String rawPassword) {}
