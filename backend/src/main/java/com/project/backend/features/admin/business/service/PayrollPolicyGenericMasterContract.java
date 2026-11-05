package com.project.backend.features.admin.business.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.masterdata.contract.GenericMasterBusinessContract;
import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;

@Component
public class PayrollPolicyGenericMasterContract
        implements GenericMasterBusinessContract {

    public static final String MASTER_CODE = "PAYROLL_POLICY";

    @Override
    public String masterCode() {
        return MASTER_CODE;
    }

    @Override
    public void validate(List<GenericMasterRecordCandidate> records) {
        List<Period> activePeriods = records.stream()
                .filter(GenericMasterRecordCandidate::activeFlag)
                .map(record -> parse(record.values()))
                .toList();
        for (int left = 0; left < activePeriods.size(); left++) {
            for (int right = left + 1; right < activePeriods.size(); right++) {
                if (overlaps(activePeriods.get(left), activePeriods.get(right))) {
                    throw new IllegalArgumentException(
                            "有効な給与制度設定の適用期間が既存設定と重複しています。"
                    );
                }
            }
        }
    }

    static Period parse(Map<String, Object> values) {
        LocalDate from = LocalDate.parse(required(values, "EFFECTIVE_FROM"));
        String toText = text(values, "EFFECTIVE_TO");
        LocalDate to = toText == null ? null : LocalDate.parse(toText);
        if (to != null && to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "適用終了日は適用開始日以降を指定してください。"
            );
        }
        DayOfWeek.valueOf(required(values, "WEEK_START_DAY"));
        positive(values, "WEEKLY_STATUTORY_HOURS", false);
        positive(values, "MONTHLY_OVERTIME_THRESHOLD_HOURS", true);
        positive(values, "OVERTIME_RATE", true);
        positive(values, "OVERTIME_OVER_THRESHOLD_RATE", true);
        positive(values, "NIGHT_PREMIUM_RATE", true);
        positive(values, "STATUTORY_HOLIDAY_RATE", true);
        positive(values, "DAILY_STANDARD_HOURS", false);
        return new Period(from, to);
    }

    private static BigDecimal positive(
            Map<String, Object> values,
            String key,
            boolean allowZero
    ) {
        BigDecimal number = new BigDecimal(required(values, key));
        if (number.signum() < 0 || (!allowZero && number.signum() == 0)) {
            throw new IllegalArgumentException(key + "は正しい正数を指定してください。");
        }
        return number;
    }

    static String required(Map<String, Object> values, String key) {
        String value = text(values, key);
        if (value == null) {
            throw new IllegalArgumentException(key + "は必須です。");
        }
        return value;
    }

    static String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value);
    }

    private boolean overlaps(Period left, Period right) {
        return (left.to() == null || !left.to().isBefore(right.from()))
                && (right.to() == null || !right.to().isBefore(left.from()));
    }

    record Period(LocalDate from, LocalDate to) {
    }
}
