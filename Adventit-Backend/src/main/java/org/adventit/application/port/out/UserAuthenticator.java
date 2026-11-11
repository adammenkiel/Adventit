package org.adventit.application.port.out;

public interface UserAuthenticator {
    String authenticate(String email, String password);
}
