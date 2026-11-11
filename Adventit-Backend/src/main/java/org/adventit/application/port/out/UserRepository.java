package org.adventit.application.port.out;

import java.util.Optional;
import org.adventit.domain.user.User;

public interface UserRepository {
    Optional<User> findByEmail(String email);

    User save(User user);
}
