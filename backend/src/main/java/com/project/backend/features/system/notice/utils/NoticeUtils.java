package com.project.backend.features.system.notice.utils;

import java.time.LocalDate;

import com.project.backend.features.dashboard.enums.NoticeType;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.NoticeDateType;

public class NoticeUtils {

    public static NoticeType toNoticeType(NoticeRule rule) {
        return switch (rule.getNoticeSeverity()) {
            case INFO -> NoticeType.INFO;
            case WARNING -> NoticeType.WARNING;
            case IMPORTANT -> NoticeType.IMPORTANT;
        };
    }

    public static String toColor(NoticeRule rule) {
        return switch (rule.getNoticeSeverity()) {
            case INFO -> "blue";
            case WARNING -> "orange";
            case IMPORTANT -> "red";
        };
    }

    public static String applyTemplate(
            String template,
            String label,
            LocalDate date,
            String key) {
        return template
                .replace("{label}", label != null ? label : "")
                .replace("{date}", date != null ? date.toString() : "")
                .replace("{key}", key != null ? key : "");
    }

    public static boolean matches(
            NoticeRule rule,
            LocalDate targetDate,
            LocalDate today) {
        NoticeDateType type = rule.getDateType();

        return switch (type) {
            case BEFORE_DAYS -> {
                int days = rule.getDaysBefore() != null ? rule.getDaysBefore() : 0;
                yield targetDate.minusDays(days).equals(today);
            }
            case EXACT_DAY -> targetDate.equals(today);
            case AFTER_DAYS -> {
                int days = rule.getDaysBefore() != null ? rule.getDaysBefore() : 0;
                yield targetDate.plusDays(days).equals(today);
            }
            case DAY_OF_MONTH -> {
                Integer day = rule.getDayOfMonth();
                yield day != null && today.getDayOfMonth() == day;
            }
            case MONTH_END -> today.equals(today.withDayOfMonth(today.lengthOfMonth()));
        };
    }
}
