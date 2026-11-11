package org.adventit.application.port.out;

import java.util.Optional;

public interface AuthenticationTokenProvider {
    String issueToken(String subject);

    Optional<String> getSubject(String token);
}
