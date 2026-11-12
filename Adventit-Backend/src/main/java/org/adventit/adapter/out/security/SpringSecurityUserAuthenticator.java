package org.adventit.adapter.out.security;

import org.adventit.application.port.out.UserAuthenticator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityUserAuthenticator implements UserAuthenticator {
    private final AuthenticationManager authenticationManager;

    public SpringSecurityUserAuthenticator(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public String authenticate(String email, String password) {
        return authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(email, password))
                .getName();
    }
}
