package microservices.ecommerce.users.infrastructure.adapters.out.mappers;

import microservices.ecommerce.users.core.entities.User;
import microservices.ecommerce.users.infrastructure.adapters.out.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/** Maps between the domain {@link User} and its JPA representation (roles stored comma-separated). */
@Component
public class UserEntityMapper {

    private static final String DEFAULT_ROLE = "USER";

    public User toDomain(UserEntity entity) {
        List<String> roles = entity.getRoles() != null && !entity.getRoles().isBlank()
                ? Arrays.stream(entity.getRoles().split(",")).map(String::trim).toList()
                : List.of(DEFAULT_ROLE);
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPassword(),
                roles,
                entity.getCreatedAt()
        );
    }

    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setRoles(user.getRoles() != null && !user.getRoles().isEmpty()
                ? String.join(",", user.getRoles())
                : DEFAULT_ROLE);
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }
}
