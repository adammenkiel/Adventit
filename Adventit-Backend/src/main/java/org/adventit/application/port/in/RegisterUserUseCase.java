package org.adventit.application.port.in;

import org.adventit.domain.user.User;

public interface RegisterUserUseCase {
    User register(String email, String password);
}
