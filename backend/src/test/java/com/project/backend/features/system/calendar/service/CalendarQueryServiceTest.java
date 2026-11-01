package com.project.backend.features.system.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.entity.SystemCalendarHoliday;
import com.project.backend.features.system.calendar.entity.SystemCalendarReference;
import com.project.backend.features.system.calendar.enums.CalendarDayType;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;
import com.project.backend.features.system.calendar.repository.SystemCalendarReferenceRepository;
import com.project.backend.features.system.calendar.repository.SystemCalendarWeekdayRuleRepository;

@ExtendWith(MockitoExtension.class)
class CalendarQueryServiceTest {
    @Mock SystemCalendarHolidayRepository holidayRepository;
    @Mock SystemCalendarWeekdayRuleRepository weekdayRuleRepository;
    @Mock SystemCalendarReferenceRepository referenceRepository;
    @Mock CalendarDefaultHolidayProvisioner defaultHolidayProvisioner;

    private CalendarQueryService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
        service = new CalendarQueryService(
                holidayRepository,
                weekdayRuleRepository,
                referenceRepository,
                defaultHolidayProvisioner
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void consumerSpecificClosedWeekdayDoesNotRequireHolidayRow() {
        CalendarDayPolicy policy = new CalendarDayPolicy(
                Set.of(CalendarType.COMPANY), Set.of(DayOfWeek.SUNDAY)
        );

        assertThat(service.isClosed(LocalDate.of(2026, 8, 16), policy)).isTrue();
    }

    @Test
    void companyWeekdayRuleClosesSelectedWeekday() {
        LocalDate date = LocalDate.of(2026, 8, 16);
        when(holidayRepository
                .findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY, date
                ))
                .thenReturn(Optional.empty());
        when(weekdayRuleRepository
                .existsByTenantIdAndCalendarTypeAndDayOfWeekAndClosedFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY, DayOfWeek.SUNDAY
                ))
                .thenReturn(true);

        assertThat(service.isClosed(
                date,
                new CalendarDayPolicy(Set.of(CalendarType.COMPANY), Set.of())
        )).isTrue();
    }

    @Test
    void companyOpenOverrideWinsOverCompanyWeekdayAndReferences() {
        LocalDate date = LocalDate.of(2026, 8, 16);
        when(holidayRepository
                .findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY, date
                ))
                .thenReturn(Optional.of(holiday(CalendarType.COMPANY, CalendarDayType.OPEN)));

        assertThat(service.isClosed(
                date,
                new CalendarDayPolicy(Set.of(CalendarType.COMPANY), Set.of())
        )).isFalse();
    }

    @Test
    void financialCalendarUsesSharedNationalHolidayRows() {
        LocalDate date = LocalDate.of(2026, 2, 11);
        when(holidayRepository
                .findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.FINANCIAL_INSTITUTION, date
                ))
                .thenReturn(Optional.empty());
        when(holidayRepository
                .findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.NATIONAL_HOLIDAY, date
                ))
                .thenReturn(Optional.of(holiday(
                        CalendarType.NATIONAL_HOLIDAY,
                        CalendarDayType.CLOSED
                )));

        assertThat(service.isClosed(
                date,
                new CalendarDayPolicy(Set.of(CalendarType.FINANCIAL_INSTITUTION), Set.of())
        )).isTrue();
    }

    @Test
    void companyCanInheritGovernmentCalendar() {
        LocalDate date = LocalDate.of(2026, 12, 29);
        when(holidayRepository
                .findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY, date
                ))
                .thenReturn(Optional.empty());
        SystemCalendarReference reference = new SystemCalendarReference();
        reference.setSourceCalendarType(CalendarType.GOVERNMENT);
        when(referenceRepository
                .findByTenantIdAndCalendarTypeAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY
                ))
                .thenReturn(List.of(reference));

        assertThat(service.isClosed(
                date,
                new CalendarDayPolicy(Set.of(CalendarType.COMPANY), Set.of())
        )).isTrue();
    }

    @Test
    void yearEndRulesDifferBetweenGovernmentAndFinancialCalendars() {
        LocalDate decemberThirtieth = LocalDate.of(2026, 12, 30);

        assertThat(service.isClosed(
                decemberThirtieth,
                new CalendarDayPolicy(Set.of(CalendarType.GOVERNMENT), Set.of())
        )).isTrue();
        assertThat(service.isClosed(
                decemberThirtieth,
                new CalendarDayPolicy(Set.of(CalendarType.FINANCIAL_INSTITUTION), Set.of())
        )).isFalse();
    }

    private SystemCalendarHoliday holiday(
            CalendarType calendarType,
            CalendarDayType dayType
    ) {
        SystemCalendarHoliday holiday = new SystemCalendarHoliday();
        holiday.setCalendarType(calendarType);
        holiday.setDayType(dayType);
        return holiday;
    }
}
