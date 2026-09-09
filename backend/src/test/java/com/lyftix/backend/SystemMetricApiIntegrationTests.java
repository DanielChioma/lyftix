package com.lyftix.backend;

import com.lyftix.backend.model.SystemMetric;
import com.lyftix.backend.repository.SystemMetricRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemMetricApiIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SystemMetricRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsAndPersistsMetricWithAuditTimestamp() throws Exception {
        createMetric("host-one", "2026-09-08T10:00:00Z")
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cpuPercent").value(25.5))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        List<SystemMetric> metrics = repository.findAll();
        assertThat(metrics).hasSize(1);
        assertThat(metrics.getFirst().getHostname()).isEqualTo("host-one");
        assertThat(metrics.getFirst().getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsBeanValidationViolations() throws Exception {
        mockMvc.perform(post("/api/system-metrics").with(csrf()).contentType(APPLICATION_JSON)
                        .content(metricJson("", "2026-09-08T10:00:00Z").replace("25.5", "101.0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void rejectsInvalidByteRelationshipsWithStandardError() throws Exception {
        String body = metricJson("host-one", "2026-09-08T10:00:00Z")
                .replace("\"memoryUsedBytes\":400", "\"memoryUsedBytes\":1001");
        mockMvc.perform(post("/api/system-metrics").with(csrf()).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid System Metric"))
                .andExpect(jsonPath("$.details[0]")
                        .value("memoryUsedBytes must not exceed memoryTotalBytes"));
    }

    @Test
    void returnsPagedMetrics() throws Exception {
        createMetric("one", "2026-09-08T10:00:00Z");
        createMetric("two", "2026-09-08T11:00:00Z");
        createMetric("three", "2026-09-08T12:00:00Z");

        mockMvc.perform(get("/api/system-metrics/paged").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].hostname").value("three"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void filtersByHostname() throws Exception {
        createMetric("wanted", "2026-09-08T10:00:00Z");
        createMetric("other", "2026-09-08T11:00:00Z");

        mockMvc.perform(get("/api/system-metrics/filter").param("hostname", "wanted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].hostname").value("wanted"));
    }

    @Test
    void filtersByInclusiveCollectedAtRange() throws Exception {
        createMetric("before", "2026-09-08T09:59:59Z");
        createMetric("start", "2026-09-08T10:00:00Z");
        createMetric("end", "2026-09-08T11:00:00Z");
        createMetric("after", "2026-09-08T11:00:01Z");

        mockMvc.perform(get("/api/system-metrics/filter")
                        .param("start", "2026-09-08T10:00:00Z")
                        .param("end", "2026-09-08T11:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].hostname").value("end"))
                .andExpect(jsonPath("$.content[1].hostname").value("start"));
    }

    @Test
    void rejectsInvalidCollectedAtRange() throws Exception {
        mockMvc.perform(get("/api/system-metrics/filter")
                        .param("start", "2026-09-08T10:00:00Z")
                        .param("end", "2026-09-08T10:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid System Metric"))
                .andExpect(jsonPath("$.details[0]").value("start must be before end"));
    }

    @Test
    void appliesFlywayV6AndPublishesOpenApiPathsAndSchemas() throws Exception {
        assertThat(jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class
        )).containsExactly("1", "2", "3", "4", "5", "6", "7");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'system_metrics' "
                        + "AND indexname IN ('idx_system_metrics_collected_at', "
                        + "'idx_system_metrics_hostname_collected_at')",
                Integer.class
        )).isEqualTo(2);

        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/system-metrics'].post").exists())
                .andExpect(jsonPath("$.paths['/api/system-metrics'].get").exists())
                .andExpect(jsonPath("$.paths['/api/system-metrics/paged'].get").exists())
                .andExpect(jsonPath("$.paths['/api/system-metrics/filter'].get").exists())
                .andExpect(jsonPath("$.components.schemas.CreateSystemMetricRequest").exists())
                .andExpect(jsonPath("$.components.schemas.SystemMetricResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists());
    }

    private org.springframework.test.web.servlet.ResultActions createMetric(
            String hostname, String collectedAt
    ) throws Exception {
        return mockMvc.perform(post("/api/system-metrics").with(csrf()).contentType(APPLICATION_JSON)
                        .content(metricJson(hostname, collectedAt)))
                .andExpect(status().isCreated());
    }

    private String metricJson(String hostname, String collectedAt) {
        return """
                {"hostname":"%s","source":"local","cpuPercent":25.5,
                 "memoryUsedBytes":400,"memoryTotalBytes":1000,
                 "diskUsedBytes":500,"diskTotalBytes":2000,
                 "loadAverage1m":1.25,"collectedAt":"%s"}
                """.formatted(hostname, collectedAt);
    }
}
