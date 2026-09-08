package com.lyftix.backend;

import com.lyftix.backend.model.GitHubActivity;
import com.lyftix.backend.repository.GitHubActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GitHubActivityApiIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubActivityRepository gitHubActivityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        gitHubActivityRepository.deleteAll();
    }

    @Test
    void createsAndPersistsActivityWithAuditTimestamps() throws Exception {
        mockMvc.perform(post("/api/github-activities")
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("PullRequestOpened", "event-1", "2026-09-01T12:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityType").value("PullRequestOpened"))
                .andExpect(jsonPath("$.repositoryName").value("lyftix"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        List<GitHubActivity> activities = gitHubActivityRepository.findAll();
        assertThat(activities).hasSize(1);
        assertThat(activities.getFirst().getExternalId()).isEqualTo("event-1");
        assertThat(activities.getFirst().getCreatedAt()).isNotNull();
        assertThat(activities.getFirst().getUpdatedAt()).isNotNull();
    }

    @Test
    void returnsStandardizedValidationErrors() throws Exception {
        mockMvc.perform(post("/api/github-activities")
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("", "", "2026-09-01T12:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsPagedActivities() throws Exception {
        createActivity("PushEvent", "event-1", "2026-09-01T12:00:00Z");
        createActivity("IssueOpened", "event-2", "2026-09-02T12:00:00Z");
        createActivity("PullRequestOpened", "event-3", "2026-09-03T12:00:00Z");

        mockMvc.perform(get("/api/github-activities/paged")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "occurredAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].externalId").value("event-3"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void filtersByOccurredAtDateRange() throws Exception {
        createActivity("PushEvent", "before", "2026-08-31T12:00:00Z");
        createActivity("PushEvent", "inside", "2026-09-02T12:00:00Z");
        createActivity("PushEvent", "after", "2026-09-05T12:00:00Z");

        mockMvc.perform(get("/api/github-activities/filter")
                        .param("start", "2026-09-01T00:00:00Z")
                        .param("end", "2026-09-04T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].externalId").value("inside"));
    }

    @Test
    void filtersByActivityType() throws Exception {
        createActivity("PushEvent", "push-1", "2026-09-01T12:00:00Z");
        createActivity("IssueOpened", "issue-1", "2026-09-02T12:00:00Z");

        mockMvc.perform(get("/api/github-activities/filter")
                        .param("activityType", "IssueOpened"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].externalId").value("issue-1"));
    }

    @Test
    void filtersByActivityTypeAndOccurredAtRange() throws Exception {
        createActivity("PushEvent", "matching", "2026-09-02T12:00:00Z");
        createActivity("IssueOpened", "wrong-type", "2026-09-02T12:00:00Z");
        createActivity("PushEvent", "wrong-date", "2026-09-05T12:00:00Z");

        mockMvc.perform(get("/api/github-activities/filter")
                        .param("activityType", "PushEvent")
                        .param("start", "2026-09-01T00:00:00Z")
                        .param("end", "2026-09-04T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].externalId").value("matching"));
    }

    @Test
    void rejectsInvalidDateRangeWithStandardizedError() throws Exception {
        mockMvc.perform(get("/api/github-activities/filter")
                        .param("start", "2026-09-04T00:00:00Z")
                        .param("end", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid GitHub Activity Filter"))
                .andExpect(jsonPath("$.details[0]").value("start must be before end"));
    }

    @Test
    void enforcesUniqueExternalId() throws Exception {
        createActivity("PushEvent", "duplicate-event", "2026-09-01T12:00:00Z");

        GitHubActivity duplicate = activity("PushEvent", "duplicate-event", "2026-09-02T12:00:00Z");

        assertThatThrownBy(() -> gitHubActivityRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void returnsConflictWhenExternalIdAlreadyExists() throws Exception {
        createActivity("PushEvent", "duplicate-api-event", "2026-09-01T12:00:00Z");

        mockMvc.perform(post("/api/github-activities")
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("IssueOpened", "duplicate-api-event", "2026-09-02T12:00:00Z")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Duplicate GitHub Activity"))
                .andExpect(jsonPath("$.details[0]")
                        .value("GitHub activity with externalId 'duplicate-api-event' already exists"));

        long matchingRows = gitHubActivityRepository.findAll()
                .stream()
                .filter(activity -> activity.getExternalId().equals("duplicate-api-event"))
                .count();
        assertThat(matchingRows).isEqualTo(1);
    }

    @Test
    void appliesGitHubActivityFlywayMigration() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class
        );
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'github_activity'",
                Integer.class
        );

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6");
        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    void documentsGitHubActivityApiInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/github-activities'].post").exists())
                .andExpect(jsonPath("$.paths['/api/github-activities'].get").exists())
                .andExpect(jsonPath("$.paths['/api/github-activities/paged'].get").exists())
                .andExpect(jsonPath("$.paths['/api/github-activities/filter'].get").exists())
                .andExpect(jsonPath("$.components.schemas.CreateGitHubActivityRequest").exists())
                .andExpect(jsonPath("$.components.schemas.GitHubActivityResponse").exists())
                .andExpect(jsonPath("$.paths['/api/github-activities'].post.responses['400'].content['*/*'].schema.$ref")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/github-activities'].post.responses['409'].content['*/*'].schema.$ref")
                        .value("#/components/schemas/ApiErrorResponse"));
    }

    private void createActivity(String activityType, String externalId, String occurredAt) throws Exception {
        mockMvc.perform(post("/api/github-activities")
                        .contentType(APPLICATION_JSON)
                        .content(activityJson(activityType, externalId, occurredAt)))
                .andExpect(status().isCreated());
    }

    private String activityJson(String activityType, String externalId, String occurredAt) {
        return """
                {
                  "activityType": "%s",
                  "repositoryName": "lyftix",
                  "repositoryOwner": "octocat",
                  "occurredAt": "%s",
                  "externalId": "%s",
                  "title": "GitHub activity"
                }
                """.formatted(activityType, occurredAt, externalId);
    }

    private GitHubActivity activity(String activityType, String externalId, String occurredAt) {
        GitHubActivity activity = new GitHubActivity();
        activity.setActivityType(activityType);
        activity.setRepositoryName("lyftix");
        activity.setRepositoryOwner("octocat");
        activity.setOccurredAt(Instant.parse(occurredAt));
        activity.setExternalId(externalId);
        activity.setTitle("GitHub activity");
        return activity;
    }
}
