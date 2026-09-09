package com.lyftix.backend;

import com.lyftix.backend.model.AppUser;
import com.lyftix.backend.model.AppUserRole;
import com.lyftix.backend.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "management.prometheus.metrics.export.enabled=true")
class AuthenticationSecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    void csrfBootstrapIsPublicAndIssuesReadableCookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void healthAndRequiredActuatorEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test
    void loginCreatesSessionAndMeReturnsOwner() throws Exception {
        createUser("owner", "strong-password", true);

        MockHttpSession session = login(" OWNER ", "strong-password");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void invalidAndUnknownCredentialsReturnSameGenericError() throws Exception {
        createUser("owner", "strong-password", true);
        String invalidPassword = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication Failed"))
                .andExpect(jsonPath("$.details[0]").value("Invalid username or password"))
                .andReturn().getResponse().getContentAsString();
        String unknownUser = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(unknownUser).contains("Authentication Failed", "Invalid username or password");
        assertThat(invalidPassword).contains("Authentication Failed", "Invalid username or password");
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        createUser("owner", "strong-password", false);
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"strong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.details[0]").value("Invalid username or password"));
    }

    @Test
    void anonymousProtectedGetAndPostReturnJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/workouts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
        mockMvc.perform(post("/api/workouts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void csrfIsRequiredAfterAuthenticationAndValidTokenReachesValidation() throws Exception {
        createUser("owner", "strong-password", true);
        MockHttpSession session = login("owner", "strong-password");

        mockMvc.perform(post("/api/workouts").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
        mockMvc.perform(post("/api/workouts").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        createUser("owner", "strong-password", true);
        MockHttpSession session = login("owner", "strong-password");

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void databaseEnforcesNormalizedUniqueUsernamesAndFlywayAppliedV7() {
        createUser("owner", "strong-password", true);
        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new AppUser("owner", passwordEncoder.encode("another-password"), AppUserRole.OWNER, true)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '7' AND success", Integer.class);
        assertThat(migrationCount).isEqualTo(1);
    }

    @Test
    void openApiDocumentsAuthEndpointsAndSessionScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/csrf'].get").exists())
                .andExpect(jsonPath("$.components.securitySchemes.sessionCookie.name").value("JSESSIONID"));
    }

    private void createUser(String username, String password, boolean enabled) {
        userRepository.saveAndFlush(new AppUser(username, passwordEncoder.encode(password), AppUserRole.OWNER, enabled));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }
}
