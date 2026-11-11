package org.adventit.domain.user;

import java.time.Instant;

public record User(long id, String email, String passwordHash, Instant createdAt) {
}
