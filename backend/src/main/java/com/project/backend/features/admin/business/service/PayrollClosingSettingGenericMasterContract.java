package com.project.backend.features.admin.business.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.common.closing.ClosingSettingDefaults;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.system.masterdata.contract.GenericMasterBusinessContract;
import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;

@Component
public class PayrollClosingSettingGenericMasterContract
        implements GenericMasterBusinessContract {

    public static final String MASTER_CODE = "PAYROLL_CLOSING_SETTING";

    @Override
    public String masterCode() {
        return MASTER_CODE;
    }

    @Override
    public void validate(List<GenericMasterRecordCandidate> records) {
        List<GenericMasterRecordCandidate> active = records.stream()
                .filter(GenericMasterRecordCandidate::activeFlag)
                .toList();
        if (active.size() != 1) {
            throw new IllegalArgumentException(
                    "給与締日設定は有効なデータを1件だけ登録してください。"
            );
        }
        GenericMasterRecordCandidate record = active.getFirst();
        String settingCode = required(record, "SETTING_CODE");
        if (!ClosingSettingDefaults.PAYROLL_SETTING_CODE.equals(settingCode)) {
            throw new IllegalArgumentException("設定コードはPAYROLLで指定してください。");
        }
        validateRule(record, "CLOSING", "締日");
        validateRule(record, "PAYMENT", "支払日");
    }

    private void validateRule(
            GenericMasterRecordCandidate record,
            String prefix,
            String label
    ) {
        DayRuleType type;
        try {
            type = DayRuleType.valueOf(required(record, prefix + "_DAY_TYPE"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(label + "の種別が不正です。");
        }
        String dayValue = optional(record, prefix + "_DAY_VALUE");
        if (type == DayRuleType.DAY_OF_MONTH) {
            int day = integer(dayValue, label + "の日");
            if (day < 1 || day > 31) {
                throw new IllegalArgumentException(label + "の日は1〜31で指定してください。");
            }
        }
        int monthOffset = integer(
                required(record, prefix + "_MONTH_OFFSET"),
                label + "の月オフセット"
        );
        if (monthOffset < -12 || monthOffset > 12) {
            throw new IllegalArgumentException(
                    label + "の月オフセットは-12〜12で指定してください。"
            );
        }
    }

    private int integer(String value, String label) {
        try {
            return new BigDecimal(value).intValueExact();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(label + "は整数で指定してください。");
        }
    }

    private String required(GenericMasterRecordCandidate record, String key) {
        String value = optional(record, key);
        if (value == null) {
            throw new IllegalArgumentException(key + "は必須です。");
        }
        return value;
    }

    private String optional(GenericMasterRecordCandidate record, String key) {
        Object value = record.values().get(key);
        return value == null || String.valueOf(value).isBlank()
                ? null
                : String.valueOf(value);
    }
}
