package com.lyftix.backend.service;

import com.lyftix.backend.dto.CodingSessionResponse;
import com.lyftix.backend.dto.CreateCodingSessionRequest;
import com.lyftix.backend.exception.InvalidCodingSessionException;
import com.lyftix.backend.model.CodingSession;
import com.lyftix.backend.repository.CodingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodingSessionServiceTests {

    private static final Instant START = Instant.parse("2026-09-01T09:00:00Z");
    private static final Instant END = Instant.parse("2026-09-01T10:30:00Z");

    @Mock private CodingSessionRepository repository;
    private CodingSessionService service;

    @BeforeEach
    void setUp() {
        service = new CodingSessionService(repository);
    }

    @Test
    void createsValidSession() {
        CodingSession saved = savedSession();
        when(repository.save(any(CodingSession.class))).thenReturn(saved);
        CodingSessionResponse response = service.createCodingSession(validRequest());
        assertThat(response.projectName()).isEqualTo("lyftix");
        verify(repository).save(any(CodingSession.class));
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> service.createCodingSession(request(START, START.minusSeconds(1))))
                .isInstanceOf(InvalidCodingSessionException.class)
                .hasMessage("endedAt must be after startedAt");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsEqualSessionTimes() {
        assertThatThrownBy(() -> service.createCodingSession(request(START, START)))
                .isInstanceOf(InvalidCodingSessionException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void derivesDurationSeconds() {
        CodingSession saved = savedSession();
        when(repository.save(any(CodingSession.class))).thenReturn(saved);
        assertThat(service.createCodingSession(validRequest()).durationSeconds()).isEqualTo(5400L);
    }

    @Test
    void mapsResponseFields() {
        CodingSession saved = savedSession();
        when(repository.save(any(CodingSession.class))).thenReturn(saved);
        CodingSessionResponse response = service.createCodingSession(validRequest());
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.language()).isEqualTo("Java");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void routesValidDateFilter() {
        stubFilter();
        service.filterCodingSessions(null, null, START, END, 0, 10, "startedAt");
        verify(repository).findByFilters(eq(null), eq(null), eq(START), eq(END), any(Pageable.class));
    }

    @Test
    void rejectsReversedFilterRange() {
        assertThatThrownBy(() -> service.filterCodingSessions(null, null, END, START, 0, 10, "startedAt"))
                .isInstanceOf(InvalidCodingSessionException.class)
                .hasMessage("start must be before end");
    }

    @Test
    void rejectsEqualFilterBoundaries() {
        assertThatThrownBy(() -> service.filterCodingSessions(null, null, START, START, 0, 10, "startedAt"))
                .isInstanceOf(InvalidCodingSessionException.class);
    }

    @Test
    void routesProjectFilter() {
        stubFilter();
        service.filterCodingSessions("lyftix", null, null, null, 0, 10, "startedAt");
        verify(repository).findByFilters(eq("lyftix"), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void routesLanguageFilter() {
        stubFilter();
        service.filterCodingSessions(null, "Java", null, null, 0, 10, "startedAt");
        verify(repository).findByFilters(eq(null), eq("Java"), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void routesCombinedFilters() {
        stubFilter();
        service.filterCodingSessions("lyftix", "Java", START, END, 0, 10, "startedAt");
        verify(repository).findByFilters(eq("lyftix"), eq("Java"), eq(START), eq(END), any(Pageable.class));
    }

    private void stubFilter() {
        when(repository.findByFilters(any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
    }

    private CreateCodingSessionRequest validRequest() {
        return request(START, END);
    }

    private CreateCodingSessionRequest request(Instant start, Instant end) {
        return new CreateCodingSessionRequest("lyftix", "Java", start, end, "manual", "notes");
    }

    private CodingSession savedSession() {
        CodingSession session = mock(CodingSession.class);
        when(session.getId()).thenReturn(42L);
        when(session.getProjectName()).thenReturn("lyftix");
        when(session.getLanguage()).thenReturn("Java");
        when(session.getStartedAt()).thenReturn(START);
        when(session.getEndedAt()).thenReturn(END);
        when(session.getSource()).thenReturn("manual");
        when(session.getNotes()).thenReturn("notes");
        when(session.getCreatedAt()).thenReturn(END.plusSeconds(1));
        when(session.getUpdatedAt()).thenReturn(END.plusSeconds(2));
        return session;
    }
}
