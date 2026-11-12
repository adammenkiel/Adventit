package org.adventit.adapter.out.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.adventit.application.port.out.AuthenticationTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationTokenService implements AuthenticationTokenProvider {
    private final Clock clock;
    private final SecretKey signingKey;
    private final Duration tokenLifetime;

    public JwtAuthenticationTokenService(
            Clock clock,
            @Value("${app.auth.jwt-secret}") String base64Secret,
            @Value("${app.auth.jwt-expiration:PT1H}") Duration tokenLifetime) {
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        if (tokenLifetime.isNegative() || tokenLifetime.isZero()) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.tokenLifetime = tokenLifetime;
    }

    @Override
    public String issueToken(String subject) {
        var now = clock.instant();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenLifetime)))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<String> getSubject(String token) {
        try {
            return Optional.ofNullable(Jwts.parser()
                            .verifyWith(signingKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload()
                            .getSubject())
                    .filter(subject -> !subject.isBlank());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
