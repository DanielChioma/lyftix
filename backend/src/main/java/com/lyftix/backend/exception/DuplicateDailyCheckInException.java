package com.lyftix.backend.exception;

import java.time.LocalDate;

public class DuplicateDailyCheckInException extends RuntimeException {

    public DuplicateDailyCheckInException(LocalDate checkInDate) {
        super("Daily check-in for date '" + checkInDate + "' already exists");
    }
}
