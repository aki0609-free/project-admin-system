package com.project.backend.features.admin.business.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
@Service
public class BusinessCalendarService {
    /**
     * 勤務日、および日払い・週払いの支払日で使用する休日判定。
     * 土曜日は通常日として扱う。
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) return false;
        return date.getDayOfWeek() == DayOfWeek.SUNDAY
                || isObonHoliday(date);
    }

    /**
     * 月払いの支払日で使用する休日判定。
     * 月払いは銀行営業日を考慮し、土日とお盆を前倒し対象にする。
     */
    public boolean isMonthlyPaymentHoliday(LocalDate date) {
        if (date == null) return false;
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || isObonHoliday(date);
    }

    public LocalDate previousBusinessDay(LocalDate date) {
        LocalDate candidate = date.minusDays(1);
        for (int i = 0; i < 366; i++, candidate = candidate.minusDays(1)) {
            if (!isHoliday(candidate)) return candidate;
        }
        throw new IllegalStateException("直近の営業日を特定できませんでした。");
    }

    public LocalDate nextBusinessDay(LocalDate date) {
        LocalDate candidate = date;
        for (int i = 0; i < 366; i++, candidate = candidate.plusDays(1)) {
            if (!isHoliday(candidate)) return candidate;
        }
        throw new IllegalStateException("次の営業日を特定できませんでした。");
    }

    public LocalDate previousMonthlyPaymentBusinessDay(LocalDate date) {
        LocalDate candidate = date;
        for (int i = 0; i < 366; i++, candidate = candidate.minusDays(1)) {
            if (!isMonthlyPaymentHoliday(candidate)) return candidate;
        }
        throw new IllegalStateException("月払いの直前営業日を特定できませんでした。");
    }

    private boolean isObonHoliday(LocalDate date) {
        return date.getMonthValue() == 8
                && date.getDayOfMonth() >= 13
                && date.getDayOfMonth() <= 15;
    }
}
