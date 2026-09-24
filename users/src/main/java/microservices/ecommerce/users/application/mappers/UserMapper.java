package microservices.ecommerce.users.application.mappers;

import microservices.ecommerce.users.core.entities.User;
import microservices.ecommerce.users.infrastructure.adapters.out.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        List<String> roles = entity.getRoles() != null && !entity.getRoles().isBlank()
                ? Arrays.asList(entity.getRoles().split(","))
                : List.of("USER");
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
        entity.setRoles(user.getRoles() != null ? String.join(",", user.getRoles()) : "USER");
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }
}
