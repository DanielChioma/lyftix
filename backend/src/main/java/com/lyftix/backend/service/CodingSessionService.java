package com.lyftix.backend.service;

import com.lyftix.backend.dto.CodingSessionResponse;
import com.lyftix.backend.dto.CreateCodingSessionRequest;
import com.lyftix.backend.exception.InvalidCodingSessionException;
import com.lyftix.backend.model.CodingSession;
import com.lyftix.backend.repository.CodingSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class CodingSessionService {

    private final CodingSessionRepository codingSessionRepository;

    public CodingSessionService(CodingSessionRepository codingSessionRepository) {
        this.codingSessionRepository = codingSessionRepository;
    }

    public CodingSessionResponse createCodingSession(CreateCodingSessionRequest request) {
        if (!request.endedAt().isAfter(request.startedAt())) {
            throw new InvalidCodingSessionException("endedAt must be after startedAt");
        }

        CodingSession session = new CodingSession();
        session.setProjectName(request.projectName());
        session.setLanguage(request.language());
        session.setStartedAt(request.startedAt());
        session.setEndedAt(request.endedAt());
        session.setSource(request.source());
        session.setNotes(request.notes());

        return toResponse(codingSessionRepository.save(session));
    }

    public List<CodingSessionResponse> getAllCodingSessions() {
        return codingSessionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<CodingSessionResponse> getCodingSessions(int page, int size, String sortBy) {
        return codingSessionRepository.findAll(pageable(page, size, sortBy))
                .map(this::toResponse);
    }

    public Page<CodingSessionResponse> filterCodingSessions(
            String projectName,
            String language,
            Instant start,
            Instant end,
            int page,
            int size,
            String sortBy
    ) {
        validateFilters(projectName, language, start, end);
        return codingSessionRepository
                .findByFilters(projectName, language, start, end, pageable(page, size, sortBy))
                .map(this::toResponse);
    }

    private void validateFilters(String projectName, String language, Instant start, Instant end) {
        if (projectName != null && projectName.isBlank()) {
            throw new InvalidCodingSessionException("projectName must not be blank");
        }
        if (language != null && language.isBlank()) {
            throw new InvalidCodingSessionException("language must not be blank");
        }
        if ((start == null) != (end == null)) {
            throw new InvalidCodingSessionException("start and end must be provided together");
        }
        if (start != null && !start.isBefore(end)) {
            throw new InvalidCodingSessionException("start must be before end");
        }
    }

    private Pageable pageable(int page, int size, String sortBy) {
        return PageRequest.of(page, size, Sort.by(sortBy).descending());
    }

    private CodingSessionResponse toResponse(CodingSession session) {
        long durationSeconds = Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds();
        return new CodingSessionResponse(
                session.getId(),
                session.getProjectName(),
                session.getLanguage(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getSource(),
                session.getNotes(),
                durationSeconds,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
