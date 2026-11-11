package org.adventit.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.adventit.application.port.in.AuthenticateUserUseCase;
import org.adventit.application.port.in.AuthenticateUserUseCase.AuthenticationResult;
import org.adventit.application.port.in.RegisterUserUseCase;
import org.adventit.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;
    private final Duration tokenLifetime;
    private final boolean secureCookie;

    public AuthController(
            RegisterUserUseCase registerUser,
            AuthenticateUserUseCase authenticateUser,
            @Value("${app.auth.jwt-expiration:PT1H}") Duration tokenLifetime,
            @Value("${app.auth.cookie-secure:false}") boolean secureCookie) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
        this.tokenLifetime = tokenLifetime;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUser.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(user.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = authenticateUser.authenticate(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authenticationCookie(result.token(), tokenLifetime).toString())
                .body(new AuthResponse(result.email()));
    }

    @GetMapping("/me")
    public AuthResponse currentUser(Authentication authentication) {
        return new AuthResponse(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authenticationCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseCookie authenticationCookie(String token, Duration maxAge) {
        return ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record AuthResponse(String email) {
    }
}
