package com.lyftix.backend;

import com.lyftix.backend.model.WorkoutMetric;
import com.lyftix.backend.repository.WorkoutMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkoutMetricApiIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutMetricRepository workoutMetricRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        workoutMetricRepository.deleteAll();
    }

    @Test
    void applicationContextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void createsAndPersistsValidWorkoutWithAuditTimestamps() throws Exception {
        mockMvc.perform(post("/api/workouts").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(workoutJson("Running", 7, 450,
                                "2026-09-01T08:00:00Z", "2026-09-01T09:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workoutType").value("Running"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        List<WorkoutMetric> workouts = workoutMetricRepository.findAll();
        assertThat(workouts).hasSize(1);
        assertThat(workouts.getFirst().getWorkoutType()).isEqualTo("Running");
        assertThat(workouts.getFirst().getCreatedAt()).isNotNull();
        assertThat(workouts.getFirst().getUpdatedAt()).isNotNull();
    }

    @Test
    void returnsBadRequestForBeanValidationFailure() throws Exception {
        mockMvc.perform(post("/api/workouts").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(workoutJson("", 11, -1,
                                "2026-09-01T08:00:00Z", "2026-09-01T09:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void returnsStandardizedErrorForInvalidWorkoutTime() throws Exception {
        mockMvc.perform(post("/api/workouts").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(workoutJson("Cycling", 5, 300,
                                "2026-09-01T09:00:00Z", "2026-09-01T08:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Workout Time"))
                .andExpect(jsonPath("$.details[0]").value("endedAt must be after startedAt"));
    }

    @Test
    void returnsPagedWorkouts() throws Exception {
        createWorkout("Running", "2026-09-01T08:00:00Z", "2026-09-01T09:00:00Z");
        createWorkout("Cycling", "2026-09-02T08:00:00Z", "2026-09-02T09:00:00Z");
        createWorkout("Swimming", "2026-09-03T08:00:00Z", "2026-09-03T09:00:00Z");

        mockMvc.perform(get("/api/workouts/paged")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "startedAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].workoutType").value("Swimming"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void filtersWithStartInclusiveAndEndExclusiveBoundaries() throws Exception {
        createWorkout("Start midnight", "2026-09-01T00:00:00Z", "2026-09-01T00:30:00Z");
        createWorkout("Late night", "2026-09-01T23:59:59.999999Z", "2026-09-02T00:30:00Z");
        createWorkout("Following midnight", "2026-09-02T00:00:00Z", "2026-09-02T01:00:00Z");

        mockMvc.perform(get("/api/workouts/filter")
                        .param("start", "2026-09-01T00:00:00Z")
                        .param("end", "2026-09-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].workoutType").value("Late night"))
                .andExpect(jsonPath("$.content[1].workoutType").value("Start midnight"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void appliesFlywayMigrationsToContainerDatabase() {
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                Integer.class
        );
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class
        );

        assertThat(successfulMigrations).isEqualTo(7);
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7");
    }

    @Test
    void exposesOpenApiDocumentationAndSwaggerUi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.info.title").value("Lyftix API"))
                .andExpect(jsonPath("$.info.description").value("Personal analytics platform API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.paths['/api/workouts'].post").exists())
                .andExpect(jsonPath("$.paths['/api/workouts'].get").exists())
                .andExpect(jsonPath("$.paths['/api/workouts/paged'].get").exists())
                .andExpect(jsonPath("$.paths['/api/workouts/filter'].get").exists())
                .andExpect(jsonPath("$.paths['/api/workouts'].post.requestBody.required").value(true))
                .andExpect(jsonPath("$.paths['/api/workouts'].post.requestBody.content['application/json'].schema.$ref")
                        .value("#/components/schemas/CreateWorkoutMetricRequest"))
                .andExpect(jsonPath("$.paths['/api/workouts'].post.responses['201'].content['*/*'].schema.$ref")
                        .value("#/components/schemas/WorkoutMetricResponse"))
                .andExpect(jsonPath("$.paths['/api/workouts'].post.responses['400'].content['*/*'].schema.$ref")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/workouts/paged'].get.parameters.length()").value(3))
                .andExpect(jsonPath("$.paths['/api/workouts/filter'].get.parameters.length()").value(5))
                .andExpect(jsonPath("$.components.schemas.CreateWorkoutMetricRequest").exists())
                .andExpect(jsonPath("$.components.schemas.WorkoutMetricResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    private void createWorkout(String workoutType, String startedAt, String endedAt) throws Exception {
        mockMvc.perform(post("/api/workouts").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(workoutJson(workoutType, 5, 300, startedAt, endedAt)))
                .andExpect(status().isCreated());
    }

    private String workoutJson(
            String workoutType,
            int intensity,
            int caloriesBurned,
            String startedAt,
            String endedAt
    ) {
        return """
                {
                  "workoutType": "%s",
                  "intensity": %d,
                  "caloriesBurned": %d,
                  "startedAt": "%s",
                  "endedAt": "%s"
                }
                """.formatted(workoutType, intensity, caloriesBurned, startedAt, endedAt);
    }
}
