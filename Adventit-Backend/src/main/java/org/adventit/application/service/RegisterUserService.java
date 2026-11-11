package org.adventit.application.service;

import java.time.Clock;
import java.util.Locale;
import org.adventit.application.port.in.RegisterUserUseCase;
import org.adventit.application.port.out.PasswordHasher;
import org.adventit.application.port.out.UserRepository;
import org.adventit.domain.user.EmailAlreadyRegisteredException;
import org.adventit.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterUserService(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public User register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        return userRepository.save(
                new User(0, normalizedEmail, passwordHasher.hash(password), clock.instant()));
    }
}
