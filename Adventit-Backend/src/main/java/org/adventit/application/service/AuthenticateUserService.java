package org.adventit.application.service;

import java.util.Locale;
import org.adventit.application.port.in.AuthenticateUserUseCase;
import org.adventit.application.port.in.AuthenticateUserUseCase.AuthenticationResult;
import org.adventit.application.port.out.AuthenticationTokenProvider;
import org.adventit.application.port.out.UserAuthenticator;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {
    private final UserAuthenticator userAuthenticator;
    private final AuthenticationTokenProvider authenticationTokenProvider;

    public AuthenticateUserService(
            UserAuthenticator userAuthenticator,
            AuthenticationTokenProvider authenticationTokenProvider) {
        this.userAuthenticator = userAuthenticator;
        this.authenticationTokenProvider = authenticationTokenProvider;
    }

    @Override
    public AuthenticationResult authenticate(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String authenticatedEmail = userAuthenticator.authenticate(normalizedEmail, password);
        return new AuthenticationResult(
                authenticatedEmail,
                authenticationTokenProvider.issueToken(authenticatedEmail));
    }
}
