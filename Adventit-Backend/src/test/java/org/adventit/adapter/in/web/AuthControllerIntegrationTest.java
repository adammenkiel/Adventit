package org.adventit.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void registrationStoresHashedPasswordAndRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"Traveler@Example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("traveler@example.com")));

        String storedPassword = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?",
                String.class,
                "traveler@example.com");
        org.junit.jupiter.api.Assertions.assertNotEquals("correct-horse", storedPassword);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"traveler@example.com","password":"another-password"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("EMAIL_ALREADY_REGISTERED")));
    }

    @Test
    void legacyAuthApiAliasIsRejected() throws Exception {
        mockMvc.perform(post("/auth/api/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"traveler@example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
    }

    @Test
    void registrationPathDoesNotRequireAuthenticationForOtherHttpMethods() throws Exception {
        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void corsAllowsCredentialedRequestsFromFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void loginReturnsAuthenticationCookieAndLogoutClearsIt() throws Exception {
        register("traveler@example.com", "correct-horse");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"traveler@example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("traveler@example.com")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andReturn();

        Cookie authenticationCookie = authenticationCookie(loginResult);
        mockMvc.perform(get("/api/auth/me").cookie(authenticationCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("traveler@example.com")));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationCookie.getValue()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/protected/resource"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/protected/resource")
                        .cookie(authenticationCookie)
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(authenticationCookie))
                .andExpect(status().isNoContent())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(
                logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        register("traveler@example.com", "correct-horse");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"traveler@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private Cookie authenticationCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String token = setCookie.substring("token=".length(), setCookie.indexOf(';'));
        return new Cookie("token", token);
    }
}
