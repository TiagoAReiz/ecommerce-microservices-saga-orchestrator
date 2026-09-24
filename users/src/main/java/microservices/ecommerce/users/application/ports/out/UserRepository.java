package microservices.ecommerce.users.application.ports.out;

import microservices.ecommerce.users.core.entities.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User save(User user);
}
