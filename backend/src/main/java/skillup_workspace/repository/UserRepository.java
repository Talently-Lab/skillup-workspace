package skillup_workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skillup_workspace.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}