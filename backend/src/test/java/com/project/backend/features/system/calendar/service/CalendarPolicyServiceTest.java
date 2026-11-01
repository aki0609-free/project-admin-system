package com.project.backend.features.system.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.dto.CalendarPolicySaveRequest;
import com.project.backend.features.system.calendar.entity.SystemCalendarReference;
import com.project.backend.features.system.calendar.entity.SystemCalendarWeekdayRule;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarReferenceRepository;
import com.project.backend.features.system.calendar.repository.SystemCalendarWeekdayRuleRepository;

@ExtendWith(MockitoExtension.class)
class CalendarPolicyServiceTest {
    @Mock SystemCalendarWeekdayRuleRepository weekdayRuleRepository;
    @Mock SystemCalendarReferenceRepository referenceRepository;
    @Mock CalendarDefaultHolidayProvisioner defaultHolidayProvisioner;

    private CalendarPolicyService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
        service = new CalendarPolicyService(
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
    void governmentPolicyIsFixedToSaturdayAndSunday() {
        var result = service.find(CalendarType.GOVERNMENT);

        assertThat(result.editable()).isFalse();
        assertThat(result.closedWeekdays())
                .containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(result.includedCalendarTypes())
                .containsExactly(CalendarType.NATIONAL_HOLIDAY);
        assertThat(result.fixedPeriods())
                .singleElement()
                .satisfies(period -> {
                    assertThat(period.startMonthDay()).isEqualTo("12-29");
                    assertThat(period.endMonthDay()).isEqualTo("01-03");
                });
    }

    @Test
    void saveSynchronizesCompanyWeekdaysAndReferences() {
        when(weekdayRuleRepository
                .findByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY
                ))
                .thenReturn(List.of());
        when(referenceRepository
                .findByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY
                ))
                .thenReturn(List.of());
        when(referenceRepository
                .findByTenantIdAndCalendarTypeAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a", CalendarType.COMPANY
                ))
                .thenReturn(List.of());

        service.save(new CalendarPolicySaveRequest(
                CalendarType.COMPANY,
                Set.of(DayOfWeek.SUNDAY),
                Set.of(CalendarType.GOVERNMENT)
        ));

        ArgumentCaptor<SystemCalendarWeekdayRule> weekdayCaptor =
                ArgumentCaptor.forClass(SystemCalendarWeekdayRule.class);
        verify(weekdayRuleRepository, org.mockito.Mockito.times(7))
                .save(weekdayCaptor.capture());
        assertThat(weekdayCaptor.getAllValues())
                .filteredOn(SystemCalendarWeekdayRule::isClosedFlag)
                .extracting(SystemCalendarWeekdayRule::getDayOfWeek)
                .containsExactly(DayOfWeek.SUNDAY);

        ArgumentCaptor<SystemCalendarReference> referenceCaptor =
                ArgumentCaptor.forClass(SystemCalendarReference.class);
        verify(referenceRepository, org.mockito.Mockito.times(2))
                .save(referenceCaptor.capture());
        assertThat(referenceCaptor.getAllValues())
                .filteredOn(SystemCalendarReference::isActiveFlag)
                .extracting(SystemCalendarReference::getSourceCalendarType)
                .containsExactly(CalendarType.GOVERNMENT);
    }

    @Test
    void companyCannotReferenceItself() {
        assertThatThrownBy(() -> service.save(new CalendarPolicySaveRequest(
                CalendarType.COMPANY,
                Set.of(),
                Set.of(CalendarType.COMPANY)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("参照できない");
    }
}
