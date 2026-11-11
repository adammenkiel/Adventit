package org.adventit.application.port.out;

public interface PasswordHasher {
    String hash(String password);
}
