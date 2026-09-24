package microservices.ecommerce.users.infrastructure.adapters.out;

import microservices.ecommerce.users.application.mappers.UserMapper;
import microservices.ecommerce.users.application.ports.out.UserRepository;
import microservices.ecommerce.users.core.entities.User;
import microservices.ecommerce.users.infrastructure.adapters.out.repositories.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper userMapper;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository, UserMapper userMapper) {
        this.jpaRepository = jpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return userMapper.toDomain(jpaRepository.save(userMapper.toEntity(user)));
    }
}
