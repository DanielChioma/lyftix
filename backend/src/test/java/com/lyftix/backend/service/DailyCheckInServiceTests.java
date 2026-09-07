package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateDailyCheckInRequest;
import com.lyftix.backend.dto.DailyCheckInResponse;
import com.lyftix.backend.exception.DuplicateDailyCheckInException;
import com.lyftix.backend.exception.InvalidDailyCheckInFilterException;
import com.lyftix.backend.model.DailyCheckIn;
import com.lyftix.backend.repository.DailyCheckInRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyCheckInServiceTests {

    private static final LocalDate START = LocalDate.parse("2026-09-01");
    private static final LocalDate END = LocalDate.parse("2026-09-07");

    @Mock
    private DailyCheckInRepository repository;

    private DailyCheckInService service;

    @BeforeEach
    void setUp() {
        service = new DailyCheckInService(repository);
    }

    @Test
    void createsValidDailyCheckIn() {
        DailyCheckIn savedCheckIn = savedCheckIn();
        when(repository.save(any(DailyCheckIn.class))).thenReturn(savedCheckIn);

        DailyCheckInResponse response = service.createDailyCheckIn(validRequest());

        assertThat(response.checkInDate()).isEqualTo(END);
        assertThat(response.mood()).isEqualTo(8);
        verify(repository).save(any(DailyCheckIn.class));
    }

    @Test
    void mapsSourceFieldsAndAuditTimestamps() {
        DailyCheckIn savedCheckIn = savedCheckIn();
        when(repository.save(any(DailyCheckIn.class))).thenReturn(savedCheckIn);

        DailyCheckInResponse response = service.createDailyCheckIn(validRequest());

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.energy()).isEqualTo(7);
        assertThat(response.focus()).isEqualTo(9);
        assertThat(response.stress()).isEqualTo(3);
        assertThat(response.sleepMinutes()).isEqualTo(480);
        assertThat(response.productivity()).isEqualTo(8);
        assertThat(response.notes()).isEqualTo("Productive day");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-09-07T20:00:00Z"));
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-09-07T20:00:01Z"));
    }

    @Test
    void acceptsValidMultiDayRangeAndRoutesToRepository() {
        when(repository.findByCheckInDateBetween(eq(START), eq(END), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(service.filterDailyCheckIns(START, END, 0, 10, "checkInDate")).isEmpty();

        verify(repository).findByCheckInDateBetween(eq(START), eq(END), any(Pageable.class));
    }

    @Test
    void acceptsSingleDayRange() {
        when(repository.findByCheckInDateBetween(eq(END), eq(END), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(service.filterDailyCheckIns(END, END, 0, 10, "checkInDate")).isEmpty();

        verify(repository).findByCheckInDateBetween(eq(END), eq(END), any(Pageable.class));
    }

    @Test
    void rejectsReversedDateRange() {
        assertThatThrownBy(() -> service.filterDailyCheckIns(END, START, 0, 10, "checkInDate"))
                .isInstanceOf(InvalidDailyCheckInFilterException.class)
                .hasMessage("startDate must be on or before endDate");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsIncompleteDateRange() {
        assertThatThrownBy(() -> service.filterDailyCheckIns(START, null, 0, 10, "checkInDate"))
                .isInstanceOf(InvalidDailyCheckInFilterException.class)
                .hasMessage("startDate and endDate must be provided together");
        verifyNoInteractions(repository);
    }

    @Test
    void translatesUniqueDateConstraintViolation() {
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "duplicate", new SQLException(), "uk_daily_check_ins_check_in_date"
        );
        when(repository.save(any(DailyCheckIn.class))).thenThrow(
                new DataIntegrityViolationException("duplicate", constraintViolation)
        );

        assertThatThrownBy(() -> service.createDailyCheckIn(validRequest()))
                .isInstanceOf(DuplicateDailyCheckInException.class)
                .hasMessage("Daily check-in for date '2026-09-07' already exists");
    }

    private CreateDailyCheckInRequest validRequest() {
        return new CreateDailyCheckInRequest(END, 8, 7, 9, 3, 480, 8, "Productive day");
    }

    private DailyCheckIn savedCheckIn() {
        DailyCheckIn checkIn = mock(DailyCheckIn.class);
        when(checkIn.getId()).thenReturn(42L);
        when(checkIn.getCheckInDate()).thenReturn(END);
        when(checkIn.getMood()).thenReturn(8);
        when(checkIn.getEnergy()).thenReturn(7);
        when(checkIn.getFocus()).thenReturn(9);
        when(checkIn.getStress()).thenReturn(3);
        when(checkIn.getSleepMinutes()).thenReturn(480);
        when(checkIn.getProductivity()).thenReturn(8);
        when(checkIn.getNotes()).thenReturn("Productive day");
        when(checkIn.getCreatedAt()).thenReturn(Instant.parse("2026-09-07T20:00:00Z"));
        when(checkIn.getUpdatedAt()).thenReturn(Instant.parse("2026-09-07T20:00:01Z"));
        return checkIn;
    }
}
