package com.project.backend.features.system.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;

@ExtendWith(MockitoExtension.class)
class CalendarQueryServiceTest {
    @Mock SystemCalendarHolidayRepository repository;
    @Mock CalendarDefaultHolidayProvisioner defaultHolidayProvisioner;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void closedWeekdayDoesNotRequireHolidayRow() {
        CalendarDayPolicy policy = new CalendarDayPolicy(
                Set.of(CalendarType.COMPANY), Set.of(DayOfWeek.SUNDAY)
        );

        assertThat(new CalendarQueryService(repository, defaultHolidayProvisioner)
                .isClosed(LocalDate.of(2026, 8, 16), policy)).isTrue();
    }

    @Test
    void oneDateCanBeClosedByAnyCalendarSelectedByConsumer() {
        LocalDate date = LocalDate.of(2026, 8, 14);
        Set<CalendarType> types = Set.of(
                CalendarType.COMPANY, CalendarType.FINANCIAL_INSTITUTION
        );
        when(repository
                .existsByTenantIdAndCalendarTypeInAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", types, date
                ))
                .thenReturn(true);

        boolean result = new CalendarQueryService(repository, defaultHolidayProvisioner).isClosed(
                date, new CalendarDayPolicy(types, Set.of())
        );

        assertThat(result).isTrue();
        verify(repository)
                .existsByTenantIdAndCalendarTypeInAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", types, date
                );
    }
}
