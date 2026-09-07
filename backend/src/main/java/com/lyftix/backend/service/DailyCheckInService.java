package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateDailyCheckInRequest;
import com.lyftix.backend.dto.DailyCheckInResponse;
import com.lyftix.backend.exception.DuplicateDailyCheckInException;
import com.lyftix.backend.exception.InvalidDailyCheckInFilterException;
import com.lyftix.backend.model.DailyCheckIn;
import com.lyftix.backend.repository.DailyCheckInRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyCheckInService {

    private static final String CHECK_IN_DATE_UNIQUE_CONSTRAINT = "uk_daily_check_ins_check_in_date";

    private final DailyCheckInRepository dailyCheckInRepository;

    public DailyCheckInService(DailyCheckInRepository dailyCheckInRepository) {
        this.dailyCheckInRepository = dailyCheckInRepository;
    }

    public DailyCheckInResponse createDailyCheckIn(CreateDailyCheckInRequest request) {
        DailyCheckIn checkIn = new DailyCheckIn();
        checkIn.setCheckInDate(request.checkInDate());
        checkIn.setMood(request.mood());
        checkIn.setEnergy(request.energy());
        checkIn.setFocus(request.focus());
        checkIn.setStress(request.stress());
        checkIn.setSleepMinutes(request.sleepMinutes());
        checkIn.setProductivity(request.productivity());
        checkIn.setNotes(request.notes());

        try {
            return toResponse(dailyCheckInRepository.save(checkIn));
        } catch (DataIntegrityViolationException exception) {
            if (isCheckInDateUniqueConstraintViolation(exception)) {
                throw new DuplicateDailyCheckInException(request.checkInDate());
            }
            throw exception;
        }
    }

    public List<DailyCheckInResponse> getAllDailyCheckIns() {
        return dailyCheckInRepository.findAll(Sort.by("checkInDate").descending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<DailyCheckInResponse> getDailyCheckIns(int page, int size, String sortBy) {
        return dailyCheckInRepository.findAll(pageable(page, size, sortBy)).map(this::toResponse);
    }

    public Page<DailyCheckInResponse> filterDailyCheckIns(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy
    ) {
        validateDateRange(startDate, endDate);
        return dailyCheckInRepository.findByCheckInDateBetween(
                startDate, endDate, pageable(page, size, sortBy)
        ).map(this::toResponse);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new InvalidDailyCheckInFilterException("startDate and endDate must be provided together");
        }
        if (startDate == null) {
            throw new InvalidDailyCheckInFilterException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidDailyCheckInFilterException("startDate must be on or before endDate");
        }
    }

    private Pageable pageable(int page, int size, String sortBy) {
        return PageRequest.of(page, size, Sort.by(sortBy).descending());
    }

    private boolean isCheckInDateUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && CHECK_IN_DATE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private DailyCheckInResponse toResponse(DailyCheckIn checkIn) {
        return new DailyCheckInResponse(
                checkIn.getId(),
                checkIn.getCheckInDate(),
                checkIn.getMood(),
                checkIn.getEnergy(),
                checkIn.getFocus(),
                checkIn.getStress(),
                checkIn.getSleepMinutes(),
                checkIn.getProductivity(),
                checkIn.getNotes(),
                checkIn.getCreatedAt(),
                checkIn.getUpdatedAt()
        );
    }
}
