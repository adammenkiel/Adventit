package org.adventit.adapter.in.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void registrationStoresHashedPasswordAndRejectsDuplicateEmail() throws Exception {
        CsrfSession csrf = newCsrfSession();

        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
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
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content("""
                                {"email":"traveler@example.com","password":"another-password"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("EMAIL_ALREADY_REGISTERED")));
    }

    @Test
    void registrationPathDoesNotRequireAuthenticationForOtherHttpMethods() throws Exception {
        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void loginCreatesAuthenticatedSessionAndLogoutEndsIt() throws Exception {
        CsrfSession csrf = newCsrfSession();
        register(csrf, "traveler@example.com", "correct-horse");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content("""
                                {"email":"traveler@example.com","password":"correct-horse"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("traveler@example.com")))
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("traveler@example.com")));

        mockMvc.perform(post("/api/auth/logout")
                        .session(authenticatedSession)
                        .header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        CsrfSession csrf = newCsrfSession();
        register(csrf, "traveler@example.com", "correct-horse");

        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content("""
                                {"email":"traveler@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    private void register(CsrfSession csrf, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AuthController.RegisterRequest(email, password))))
                .andExpect(status().isCreated());
    }

    private CsrfSession newCsrfSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfSession(
                (MockHttpSession) result.getRequest().getSession(false),
                response.get("token").asText());
    }

    private record CsrfSession(MockHttpSession session, String token) {
    }
}
