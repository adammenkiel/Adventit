package org.adventit.application.port.in;

public interface AuthenticateUserUseCase {
    AuthenticationResult authenticate(String email, String password);

    record AuthenticationResult(String email, String token) {
    }
}
