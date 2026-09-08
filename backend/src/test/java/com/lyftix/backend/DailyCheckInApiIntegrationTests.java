package com.lyftix.backend;

import com.lyftix.backend.model.DailyCheckIn;
import com.lyftix.backend.repository.DailyCheckInRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DailyCheckInApiIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DailyCheckInRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsAndPersistsCheckInWithAuditTimestamps() throws Exception {
        createCheckIn("2026-09-07", 8, 7, 9, 3, 480, 8)
                .andExpect(jsonPath("$.checkInDate").value("2026-09-07"))
                .andExpect(jsonPath("$.mood").value(8))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        List<DailyCheckIn> checkIns = repository.findAll();
        assertThat(checkIns).hasSize(1);
        assertThat(checkIns.getFirst().getCheckInDate().toString()).isEqualTo("2026-09-07");
        assertThat(checkIns.getFirst().getCreatedAt()).isNotNull();
        assertThat(checkIns.getFirst().getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsRatingBelowLowerBound() throws Exception {
        assertValidationError(checkInJson("2026-09-07", 0, 7, 9, 3, 480, 8));
    }

    @Test
    void rejectsRatingAboveUpperBound() throws Exception {
        assertValidationError(checkInJson("2026-09-07", 8, 11, 9, 3, 480, 8));
    }

    @Test
    void rejectsInvalidSleepMinutes() throws Exception {
        assertValidationError(checkInJson("2026-09-07", 8, 7, 9, 3, 1441, 8));
    }

    @Test
    void returnsConflictForDuplicateDateAndKeepsOneRow() throws Exception {
        createCheckIn("2026-09-07", 8, 7, 9, 3, 480, 8);

        mockMvc.perform(post("/api/daily-check-ins").contentType(APPLICATION_JSON)
                        .content(checkInJson("2026-09-07", 4, 4, 4, 4, 420, 4)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Duplicate Daily Check-in"))
                .andExpect(jsonPath("$.details[0]")
                        .value("Daily check-in for date '2026-09-07' already exists"));

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void returnsPagedCheckInsInDescendingDateOrder() throws Exception {
        createCheckIn("2026-09-01", 5, 5, 5, 5, 420, 5);
        createCheckIn("2026-09-02", 6, 6, 6, 6, 430, 6);
        createCheckIn("2026-09-03", 7, 7, 7, 7, 440, 7);

        mockMvc.perform(get("/api/daily-check-ins/paged").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].checkInDate").value("2026-09-03"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void filtersAcrossInclusiveMultiDayRange() throws Exception {
        createCheckIn("2026-09-01", 5, 5, 5, 5, 420, 5);
        createCheckIn("2026-09-03", 6, 6, 6, 6, 430, 6);
        createCheckIn("2026-09-05", 7, 7, 7, 7, 440, 7);

        mockMvc.perform(get("/api/daily-check-ins/filter")
                        .param("startDate", "2026-09-02").param("endDate", "2026-09-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].checkInDate").value("2026-09-03"));
    }

    @Test
    void filtersSingleCalendarDay() throws Exception {
        createCheckIn("2026-09-06", 6, 6, 6, 6, 430, 6);
        createCheckIn("2026-09-07", 7, 7, 7, 7, 440, 7);

        mockMvc.perform(get("/api/daily-check-ins/filter")
                        .param("startDate", "2026-09-07").param("endDate", "2026-09-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].checkInDate").value("2026-09-07"));
    }

    @Test
    void rejectsReversedDateRangeWithStandardizedError() throws Exception {
        mockMvc.perform(get("/api/daily-check-ins/filter")
                        .param("startDate", "2026-09-07").param("endDate", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Daily Check-in Filter"))
                .andExpect(jsonPath("$.details[0]").value("startDate must be on or before endDate"));
    }

    @Test
    void rejectsIncompleteDateRangeWithStandardizedError() throws Exception {
        mockMvc.perform(get("/api/daily-check-ins/filter").param("startDate", "2026-09-07"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Daily Check-in Filter"))
                .andExpect(jsonPath("$.details[0]").value("startDate and endDate must be provided together"));
    }

    @Test
    void appliesV5MigrationAndCreatesNamedConstraints() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class
        )).containsExactly("1", "2", "3", "4", "5", "6");

        List<String> constraints = jdbcTemplate.queryForList("""
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE table_schema = 'public' AND table_name = 'daily_check_ins'
                """, String.class);
        assertThat(constraints).contains(
                "uk_daily_check_ins_check_in_date",
                "ck_daily_check_ins_mood_range",
                "ck_daily_check_ins_energy_range",
                "ck_daily_check_ins_focus_range",
                "ck_daily_check_ins_stress_range",
                "ck_daily_check_ins_sleep_minutes_range",
                "ck_daily_check_ins_productivity_range",
                "ck_daily_check_ins_notes_length"
        );
    }

    @Test
    void databaseRejectsInvalidRatingsWhenBeanValidationIsBypassed() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO daily_check_ins
                    (check_in_date, mood, energy, focus, stress, sleep_minutes, productivity)
                VALUES (DATE '2026-09-07', 0, 7, 9, 3, 480, 8)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void documentsDailyCheckInApiAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/daily-check-ins'].post").exists())
                .andExpect(jsonPath("$.paths['/api/daily-check-ins'].get").exists())
                .andExpect(jsonPath("$.paths['/api/daily-check-ins/paged'].get").exists())
                .andExpect(jsonPath("$.paths['/api/daily-check-ins/filter'].get").exists())
                .andExpect(jsonPath("$.components.schemas.CreateDailyCheckInRequest").exists())
                .andExpect(jsonPath("$.components.schemas.DailyCheckInResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CreateDailyCheckInRequest.properties.checkInDate.format")
                        .value("date"))
                .andExpect(jsonPath("$.paths['/api/daily-check-ins'].post.responses['409'].content['*/*'].schema.$ref")
                        .value("#/components/schemas/ApiErrorResponse"));
    }

    private org.springframework.test.web.servlet.ResultActions createCheckIn(
            String date, int mood, int energy, int focus, int stress, int sleepMinutes, int productivity
    ) throws Exception {
        return mockMvc.perform(post("/api/daily-check-ins").contentType(APPLICATION_JSON)
                        .content(checkInJson(date, mood, energy, focus, stress, sleepMinutes, productivity)))
                .andExpect(status().isCreated());
    }

    private void assertValidationError(String content) throws Exception {
        mockMvc.perform(post("/api/daily-check-ins").contentType(APPLICATION_JSON).content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    private String checkInJson(
            String date, int mood, int energy, int focus, int stress, int sleepMinutes, int productivity
    ) {
        return """
                {"checkInDate":"%s","mood":%d,"energy":%d,"focus":%d,"stress":%d,
                 "sleepMinutes":%d,"productivity":%d,"notes":"Daily reflection"}
                """.formatted(date, mood, energy, focus, stress, sleepMinutes, productivity);
    }
}
