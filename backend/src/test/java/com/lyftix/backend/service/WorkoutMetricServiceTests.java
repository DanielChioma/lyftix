package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateWorkoutMetricRequest;
import com.lyftix.backend.dto.WorkoutMetricResponse;
import com.lyftix.backend.exception.InvalidWorkoutTimeException;
import com.lyftix.backend.model.WorkoutMetric;
import com.lyftix.backend.repository.WorkoutMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutMetricServiceTests {

    private static final Instant STARTED_AT = Instant.parse("2026-09-01T08:00:00Z");
    private static final Instant ENDED_AT = Instant.parse("2026-09-01T09:00:00Z");

    @Mock
    private WorkoutMetricRepository workoutMetricRepository;

    private WorkoutMetricService workoutMetricService;

    @BeforeEach
    void setUp() {
        workoutMetricService = new WorkoutMetricService(workoutMetricRepository);
    }

    @Test
    void createsValidWorkout() {
        WorkoutMetric savedWorkout = savedWorkout();
        when(workoutMetricRepository.save(any(WorkoutMetric.class))).thenReturn(savedWorkout);

        WorkoutMetricResponse response = workoutMetricService.createWorkoutMetric(validRequest());

        assertThat(response.workoutType()).isEqualTo("Running");
        assertThat(response.intensity()).isEqualTo(7);
        assertThat(response.caloriesBurned()).isEqualTo(450);
        verify(workoutMetricRepository).save(any(WorkoutMetric.class));
    }

    @Test
    void rejectsWorkoutEndingBeforeItStarts() {
        CreateWorkoutMetricRequest request = request(STARTED_AT, STARTED_AT.minusSeconds(1));

        assertThatThrownBy(() -> workoutMetricService.createWorkoutMetric(request))
                .isInstanceOf(InvalidWorkoutTimeException.class)
                .hasMessage("endedAt must be after startedAt");
        verifyNoInteractions(workoutMetricRepository);
    }

    @Test
    void rejectsWorkoutWithEqualStartAndEnd() {
        CreateWorkoutMetricRequest request = request(STARTED_AT, STARTED_AT);

        assertThatThrownBy(() -> workoutMetricService.createWorkoutMetric(request))
                .isInstanceOf(InvalidWorkoutTimeException.class)
                .hasMessage("endedAt must be after startedAt");
        verifyNoInteractions(workoutMetricRepository);
    }

    @Test
    void retrievesWorkoutsForValidDateFilterRange() {
        when(workoutMetricRepository.findByStartedAtBetween(any(), any(), any()))
                .thenReturn(Page.empty());

        Page<WorkoutMetricResponse> response = workoutMetricService.getWorkoutMetricsByDateRange(
                STARTED_AT, ENDED_AT, 0, 10, "startedAt"
        );

        assertThat(response).isEmpty();
        verify(workoutMetricRepository).findByStartedAtBetween(any(), any(), any());
    }

    @Test
    void rejectsReversedDateFilterRange() {
        assertThatThrownBy(() -> workoutMetricService.getWorkoutMetricsByDateRange(
                ENDED_AT, STARTED_AT, 0, 10, "startedAt"
        ))
                .isInstanceOf(InvalidWorkoutTimeException.class)
                .hasMessage("start must be before end");
        verify(workoutMetricRepository, never()).findByStartedAtBetween(any(), any(), any());
    }

    @Test
    void rejectsEqualDateFilterBoundaries() {
        assertThatThrownBy(() -> workoutMetricService.getWorkoutMetricsByDateRange(
                STARTED_AT, STARTED_AT, 0, 10, "startedAt"
        ))
                .isInstanceOf(InvalidWorkoutTimeException.class)
                .hasMessage("start must be before end");
        verify(workoutMetricRepository, never()).findByStartedAtBetween(any(), any(), any());
    }

    @Test
    void mapsIdentityAndAuditTimestampsIntoResponse() {
        WorkoutMetric savedWorkout = savedWorkout();
        when(workoutMetricRepository.save(any(WorkoutMetric.class))).thenReturn(savedWorkout);

        WorkoutMetricResponse response = workoutMetricService.createWorkoutMetric(validRequest());

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-09-01T10:05:00Z"));
    }

    private CreateWorkoutMetricRequest validRequest() {
        return request(STARTED_AT, ENDED_AT);
    }

    private CreateWorkoutMetricRequest request(Instant startedAt, Instant endedAt) {
        return new CreateWorkoutMetricRequest("Running", 7, 450, startedAt, endedAt);
    }

    private WorkoutMetric savedWorkout() {
        WorkoutMetric workoutMetric = mock(WorkoutMetric.class);
        when(workoutMetric.getId()).thenReturn(42L);
        when(workoutMetric.getWorkoutType()).thenReturn("Running");
        when(workoutMetric.getIntensity()).thenReturn(7);
        when(workoutMetric.getCaloriesBurned()).thenReturn(450);
        when(workoutMetric.getStartedAt()).thenReturn(STARTED_AT);
        when(workoutMetric.getEndedAt()).thenReturn(ENDED_AT);
        when(workoutMetric.getCreatedAt()).thenReturn(Instant.parse("2026-09-01T10:00:00Z"));
        when(workoutMetric.getUpdatedAt()).thenReturn(Instant.parse("2026-09-01T10:05:00Z"));
        return workoutMetric;
    }
}
