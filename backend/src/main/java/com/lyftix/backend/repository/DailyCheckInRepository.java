package com.lyftix.backend.repository;

import com.lyftix.backend.model.DailyCheckIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {

    Page<DailyCheckIn> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query(value = """
            SELECT AVG(mood)::double precision AS "averageMood",
                   AVG(energy)::double precision AS "averageEnergy",
                   AVG(focus)::double precision AS "averageFocus",
                   AVG(stress)::double precision AS "averageStress",
                   AVG(productivity)::double precision AS "averageProductivity",
                   AVG(sleep_minutes)::double precision AS "averageSleepMinutes"
            FROM daily_check_ins
            WHERE check_in_date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    CheckInAverages aggregateAverages(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            SELECT check_in_date AS date, mood, energy, focus, stress, productivity, sleep_minutes AS "sleepMinutes"
            FROM daily_check_ins
            WHERE check_in_date BETWEEN :startDate AND :endDate
            ORDER BY check_in_date
            """, nativeQuery = true)
    List<DailyTrend> findDailyTrends(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            SELECT date_trunc(CAST(:period AS text), check_in_date::timestamp)::date AS "periodStart",
                   AVG(mood)::double precision AS "averageMood",
                   AVG(energy)::double precision AS "averageEnergy",
                   AVG(focus)::double precision AS "averageFocus",
                   AVG(stress)::double precision AS "averageStress",
                   AVG(productivity)::double precision AS "averageProductivity",
                   AVG(sleep_minutes)::double precision AS "averageSleepMinutes"
            FROM daily_check_ins
            WHERE check_in_date BETWEEN :startDate AND :endDate
            GROUP BY "periodStart" ORDER BY "periodStart"
            """, nativeQuery = true)
    List<CheckInPeriodAverages> aggregateByPeriod(
            @Param("period") String period,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    interface CheckInAverages {
        Double getAverageMood();
        Double getAverageEnergy();
        Double getAverageFocus();
        Double getAverageStress();
        Double getAverageProductivity();
        Double getAverageSleepMinutes();
    }

    interface DailyTrend {
        LocalDate getDate();
        Integer getMood();
        Integer getEnergy();
        Integer getFocus();
        Integer getStress();
        Integer getProductivity();
        Integer getSleepMinutes();
    }

    interface CheckInPeriodAverages {
        LocalDate getPeriodStart();
        Double getAverageMood();
        Double getAverageEnergy();
        Double getAverageFocus();
        Double getAverageStress();
        Double getAverageProductivity();
        Double getAverageSleepMinutes();
    }
}
