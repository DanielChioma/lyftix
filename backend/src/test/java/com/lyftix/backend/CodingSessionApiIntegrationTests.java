package com.lyftix.backend;

import com.lyftix.backend.model.CodingSession;
import com.lyftix.backend.repository.CodingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CodingSessionApiIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CodingSessionRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsAndPersistsSessionWithAuditAndDerivedDuration() throws Exception {
        createSession("lyftix", "Java", "2026-09-01T09:00:00Z", "2026-09-01T10:30:00Z")
                .andExpect(jsonPath("$.durationSeconds").value(5400))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        List<CodingSession> sessions = repository.findAll();
        assertThat(sessions).hasSize(1);
        assertThat(sessions.getFirst().getProjectName()).isEqualTo("lyftix");
        assertThat(sessions.getFirst().getCreatedAt()).isNotNull();
    }

    @Test
    void returnsStandardizedBeanValidationError() throws Exception {
        mockMvc.perform(post("/api/coding-sessions").contentType(APPLICATION_JSON)
                        .content(sessionJson("", "", "2026-09-01T09:00:00Z", "2026-09-01T10:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsStandardizedInvalidSessionTimeError() throws Exception {
        mockMvc.perform(post("/api/coding-sessions").contentType(APPLICATION_JSON)
                        .content(sessionJson("lyftix", "Java", "2026-09-01T10:00:00Z", "2026-09-01T09:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Coding Session"))
                .andExpect(jsonPath("$.details[0]").value("endedAt must be after startedAt"));
    }

    @Test
    void returnsPagedSessions() throws Exception {
        createSession("one", "Java", "2026-09-01T09:00:00Z", "2026-09-01T10:00:00Z");
        createSession("two", "Java", "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        createSession("three", "Java", "2026-09-03T09:00:00Z", "2026-09-03T10:00:00Z");
        mockMvc.perform(get("/api/coding-sessions/paged").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].projectName").value("three"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void filtersByDateRange() throws Exception {
        createSession("before", "Java", "2026-08-31T09:00:00Z", "2026-08-31T10:00:00Z");
        createSession("inside", "Java", "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        mockMvc.perform(get("/api/coding-sessions/filter")
                        .param("start", "2026-09-01T00:00:00Z").param("end", "2026-09-03T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].projectName").value("inside"));
    }

    @Test
    void usesStartInclusiveEndExclusiveBoundariesAtPostgresPrecision() throws Exception {
        createSession("start", "Java", "2026-09-01T00:00:00Z", "2026-09-01T00:01:00Z");
        createSession("late", "Java", "2026-09-02T23:59:59.999999Z", "2026-09-03T00:00:59.999999Z");
        createSession("following", "Java", "2026-09-03T00:00:00Z", "2026-09-03T00:01:00Z");
        mockMvc.perform(get("/api/coding-sessions/filter").param("start", "2026-09-01T00:00:00Z").param("end", "2026-09-03T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].projectName").value(org.hamcrest.Matchers.containsInAnyOrder("start", "late")));
    }

    @Test
    void rejectsEqualFilterRange() throws Exception {
        mockMvc.perform(get("/api/coding-sessions/filter").param("start", "2026-09-01T00:00:00Z").param("end", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details[0]").value("start must be before end"));
    }

    @Test
    void filtersByProject() throws Exception {
        createSession("lyftix", "Java", "2026-09-01T09:00:00Z", "2026-09-01T10:00:00Z");
        createSession("other", "Java", "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        mockMvc.perform(get("/api/coding-sessions/filter").param("projectName", "lyftix"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].projectName").value("lyftix"));
    }

    @Test
    void filtersByLanguage() throws Exception {
        createSession("lyftix", "Java", "2026-09-01T09:00:00Z", "2026-09-01T10:00:00Z");
        createSession("lyftix", "Python", "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        mockMvc.perform(get("/api/coding-sessions/filter").param("language", "Python"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].language").value("Python"));
    }

    @Test
    void appliesCombinedFilters() throws Exception {
        createSession("lyftix", "Java", "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        createSession("other", "Java", "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        createSession("lyftix", "Python", "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        mockMvc.perform(get("/api/coding-sessions/filter").param("projectName", "lyftix")
                        .param("language", "Java").param("start", "2026-09-01T00:00:00Z")
                        .param("end", "2026-09-03T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void rejectsInvalidFilterRange() throws Exception {
        mockMvc.perform(get("/api/coding-sessions/filter")
                        .param("start", "2026-09-03T00:00:00Z").param("end", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Coding Session"))
                .andExpect(jsonPath("$.details[0]").value("start must be before end"));
    }

    @Test
    void appliesMigrationAndPublishesOpenApiSchemas() throws Exception {
        assertThat(jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class))
                .containsExactly("1", "2", "3", "4", "5", "6");
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/coding-sessions'].post").exists())
                .andExpect(jsonPath("$.paths['/api/coding-sessions'].get").exists())
                .andExpect(jsonPath("$.paths['/api/coding-sessions/paged'].get").exists())
                .andExpect(jsonPath("$.paths['/api/coding-sessions/filter'].get").exists())
                .andExpect(jsonPath("$.components.schemas.CreateCodingSessionRequest").exists())
                .andExpect(jsonPath("$.components.schemas.CodingSessionResponse.properties.durationSeconds").exists());
    }

    private org.springframework.test.web.servlet.ResultActions createSession(
            String project, String language, String start, String end) throws Exception {
        return mockMvc.perform(post("/api/coding-sessions").contentType(APPLICATION_JSON)
                        .content(sessionJson(project, language, start, end)))
                .andExpect(status().isCreated());
    }

    private String sessionJson(String project, String language, String start, String end) {
        return """
                {"projectName":"%s","language":"%s","startedAt":"%s","endedAt":"%s",
                 "source":"manual","notes":"Focused coding"}
                """.formatted(project, language, start, end);
    }
}
