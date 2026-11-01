package com.project.backend.features.system.calendar.service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.dto.CalendarEffectiveHolidayResponse;
import com.project.backend.features.system.calendar.entity.SystemCalendarHoliday;
import com.project.backend.features.system.calendar.enums.CalendarDayType;
import com.project.backend.features.system.calendar.enums.CalendarHolidaySource;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarEffectiveHolidayService {
    private final SystemCalendarHolidayRepository repository;

    public List<CalendarEffectiveHolidayResponse> findByYear(
            CalendarType type,
            int year
    ) {
        validate(type, year);
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        Map<LocalDate, EffectiveHoliday> result = new TreeMap<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (CalendarFixedRuleCatalog.closedWeekdays(type).contains(date.getDayOfWeek())) {
                add(result, date, weekdayName(date), CalendarHolidaySource.WEEKDAY_RULE, false);
            }
            for (var period : CalendarFixedRuleCatalog.fixedPeriods(type)) {
                if (period.contains(date)) {
                    add(
                            result,
                            date,
                            period.name(),
                            CalendarHolidaySource.YEAR_END_NEW_YEAR,
                            true
                    );
                }
            }
        }

        stored(CalendarType.NATIONAL_HOLIDAY, from, to).forEach(item -> add(
                result,
                item.getHolidayDate(),
                item.getHolidayName(),
                CalendarHolidaySource.NATIONAL_HOLIDAY,
                true
        ));
        stored(type, from, to).forEach(item -> add(
                result,
                item.getHolidayDate(),
                item.getHolidayName(),
                CalendarHolidaySource.INDIVIDUAL,
                true
        ));

        return result.entrySet().stream()
                .map(entry -> new CalendarEffectiveHolidayResponse(
                        entry.getKey(),
                        entry.getValue().name,
                        Set.copyOf(entry.getValue().sources)
                ))
                .toList();
    }

    private List<SystemCalendarHoliday> stored(
            CalendarType type,
            LocalDate from,
            LocalDate to
    ) {
        return repository
                .findByTenantIdAndCalendarTypeAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                        TenantContext.getTenantId(), type, from, to
                )
                .stream()
                .filter(SystemCalendarHoliday::isActiveFlag)
                .filter(item -> item.getDayType() == CalendarDayType.CLOSED)
                .toList();
    }

    private void add(
            Map<LocalDate, EffectiveHoliday> result,
            LocalDate date,
            String name,
            CalendarHolidaySource source,
            boolean preferredName
    ) {
        EffectiveHoliday holiday = result.computeIfAbsent(
                date,
                ignored -> new EffectiveHoliday(name)
        );
        holiday.sources.add(source);
        if (preferredName) holiday.name = name;
    }

    private String weekdayName(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> "土曜日";
            case SUNDAY -> "日曜日";
            default -> date.getDayOfWeek().name();
        };
    }

    private void validate(CalendarType type, int year) {
        if (type != CalendarType.GOVERNMENT
                && type != CalendarType.FINANCIAL_INSTITUTION) {
            throw new IllegalArgumentException("実効休日を表示できないカレンダー種別です。");
        }
        if (year < 2000 || year > 2200) {
            throw new IllegalArgumentException("対象年が不正です。");
        }
    }

    private static final class EffectiveHoliday {
        private String name;
        private final EnumSet<CalendarHolidaySource> sources =
                EnumSet.noneOf(CalendarHolidaySource.class);

        private EffectiveHoliday(String name) {
            this.name = name;
        }
    }
}
