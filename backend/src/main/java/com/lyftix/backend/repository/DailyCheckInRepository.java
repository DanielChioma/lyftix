package com.lyftix.backend.repository;

import com.lyftix.backend.model.DailyCheckIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {

    Page<DailyCheckIn> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
}
