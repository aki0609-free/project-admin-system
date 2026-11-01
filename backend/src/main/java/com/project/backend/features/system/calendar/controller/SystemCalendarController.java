package com.project.backend.features.system.calendar.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.calendar.dto.CalendarHolidayResponse;
import com.project.backend.features.system.calendar.dto.CalendarHolidaySaveRequest;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.service.CalendarHolidayManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/calendars")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class SystemCalendarController {
    private final CalendarHolidayManagementService service;

    @GetMapping("/holidays")
    public List<CalendarHolidayResponse> findHolidays(
            @RequestParam CalendarType calendarType,
            @RequestParam int year
    ) {
        return service.findByYear(calendarType, year);
    }

    @PostMapping("/holidays")
    public CalendarHolidayResponse saveHoliday(
            @Valid @RequestBody CalendarHolidaySaveRequest request
    ) {
        return service.save(request);
    }

    @DeleteMapping("/holidays/{id}")
    public void deleteHoliday(@PathVariable Long id) {
        service.delete(id);
    }
}
