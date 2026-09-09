package com.lyftix.backend.controller;

import com.lyftix.backend.dto.CreateDailyCheckInRequest;
import com.lyftix.backend.dto.DailyCheckInResponse;
import com.lyftix.backend.exception.ApiErrorResponse;
import com.lyftix.backend.service.DailyCheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-check-ins")
@Tag(name = "Daily Check-ins", description = "Record and retrieve daily wellbeing and productivity signals")
@SecurityRequirement(name = "sessionCookie")
public class DailyCheckInController {

    private final DailyCheckInService dailyCheckInService;

    public DailyCheckInController(DailyCheckInService dailyCheckInService) {
        this.dailyCheckInService = dailyCheckInService;
    }

    @Operation(summary = "Create a daily check-in", description = "Records one check-in for a calendar date")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Daily check-in created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A check-in already exists for the supplied date",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DailyCheckInResponse createDailyCheckIn(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Daily ratings and sleep duration to persist",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateDailyCheckInRequest.class)))
            @Valid @RequestBody CreateDailyCheckInRequest request
    ) {
        return dailyCheckInService.createDailyCheckIn(request);
    }

    @Operation(summary = "List all daily check-ins", description = "Returns all check-ins newest calendar date first")
    @ApiResponse(responseCode = "200", description = "Daily check-ins returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DailyCheckInResponse.class))))
    @GetMapping
    public List<DailyCheckInResponse> getAllDailyCheckIns() {
        return dailyCheckInService.getAllDailyCheckIns();
    }

    @Operation(summary = "List paged daily check-ins", description = "Returns a descending, property-sorted page of check-ins")
    @ApiResponse(responseCode = "200", description = "Daily check-in page returned", useReturnTypeSchema = true)
    @GetMapping("/paged")
    public Page<DailyCheckInResponse> getDailyCheckIns(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of check-ins per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "DailyCheckIn property used for descending sorting", example = "checkInDate")
            @RequestParam(defaultValue = "checkInDate") String sortBy
    ) {
        return dailyCheckInService.getDailyCheckIns(page, size, sortBy);
    }

    @Operation(summary = "Filter daily check-ins",
            description = "Returns check-ins in an inclusive calendar-date range; both boundaries are required")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered daily check-in page returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Incomplete or reversed calendar-date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/filter")
    public Page<DailyCheckInResponse> filterDailyCheckIns(
            @Parameter(description = "Inclusive lower calendar-date boundary", example = "2026-09-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive upper calendar-date boundary", example = "2026-09-07")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of check-ins per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "DailyCheckIn property used for descending sorting", example = "checkInDate")
            @RequestParam(defaultValue = "checkInDate") String sortBy
    ) {
        return dailyCheckInService.filterDailyCheckIns(startDate, endDate, page, size, sortBy);
    }
}
