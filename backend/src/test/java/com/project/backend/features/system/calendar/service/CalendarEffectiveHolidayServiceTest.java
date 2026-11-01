package com.project.backend.features.system.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.entity.SystemCalendarHoliday;
import com.project.backend.features.system.calendar.enums.CalendarDayType;
import com.project.backend.features.system.calendar.enums.CalendarHolidaySource;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;

@ExtendWith(MockitoExtension.class)
class CalendarEffectiveHolidayServiceTest {
    @Mock SystemCalendarHolidayRepository repository;

    private CalendarEffectiveHolidayService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
        service = new CalendarEffectiveHolidayService(repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void governmentCalendarShowsMergedSourcesAndItsLongerYearEndPeriod() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        when(repository
                .findByTenantIdAndCalendarTypeAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                        "tenant-a", CalendarType.NATIONAL_HOLIDAY, from, to
                ))
                .thenReturn(List.of(holiday(
                        CalendarType.NATIONAL_HOLIDAY,
                        LocalDate.of(2026, 1, 1),
                        "元日"
                )));
        when(repository
                .findByTenantIdAndCalendarTypeAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                        "tenant-a", CalendarType.GOVERNMENT, from, to
                ))
                .thenReturn(List.of(holiday(
                        CalendarType.GOVERNMENT,
                        LocalDate.of(2026, 5, 1),
                        "個別閉庁日"
                )));

        var result = service.findByYear(CalendarType.GOVERNMENT, 2026);

        assertThat(result)
                .filteredOn(item -> item.holidayDate().equals(LocalDate.of(2026, 1, 1)))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.holidayName()).isEqualTo("元日");
                    assertThat(item.sources()).contains(
                            CalendarHolidaySource.NATIONAL_HOLIDAY,
                            CalendarHolidaySource.YEAR_END_NEW_YEAR
                    );
                });
        assertThat(result)
                .anySatisfy(item -> {
                    assertThat(item.holidayDate()).isEqualTo(LocalDate.of(2026, 12, 30));
                    assertThat(item.sources()).contains(
                            CalendarHolidaySource.YEAR_END_NEW_YEAR
                    );
                })
                .anySatisfy(item -> {
                    assertThat(item.holidayDate()).isEqualTo(LocalDate.of(2026, 5, 1));
                    assertThat(item.sources()).contains(CalendarHolidaySource.INDIVIDUAL);
                });
    }

    @Test
    void financialCalendarDoesNotCloseDecemberThirtiethByFixedRule() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        when(repository
                .findByTenantIdAndCalendarTypeAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                        "tenant-a", CalendarType.NATIONAL_HOLIDAY, from, to
                ))
                .thenReturn(List.of());
        when(repository
                .findByTenantIdAndCalendarTypeAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                        "tenant-a", CalendarType.FINANCIAL_INSTITUTION, from, to
                ))
                .thenReturn(List.of());

        var result = service.findByYear(CalendarType.FINANCIAL_INSTITUTION, 2026);

        assertThat(result)
                .noneMatch(item -> item.holidayDate().equals(LocalDate.of(2026, 12, 30)))
                .anyMatch(item -> item.holidayDate().equals(LocalDate.of(2026, 12, 31)));
    }

    private SystemCalendarHoliday holiday(
            CalendarType type,
            LocalDate date,
            String name
    ) {
        SystemCalendarHoliday holiday = new SystemCalendarHoliday();
        holiday.setCalendarType(type);
        holiday.setHolidayDate(date);
        holiday.setHolidayName(name);
        holiday.setDayType(CalendarDayType.CLOSED);
        holiday.setActiveFlag(true);
        return holiday;
    }
}
