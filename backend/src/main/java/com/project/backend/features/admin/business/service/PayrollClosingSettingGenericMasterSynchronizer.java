package com.project.backend.features.admin.business.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.common.closing.ClosingSettingDefaults;
import com.project.backend.common.closing.entity.ClosingSetting;
import com.project.backend.common.closing.repository.ClosingSettingRepository;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;
import com.project.backend.features.system.masterdata.contract.GenericMasterRecordSynchronizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PayrollClosingSettingGenericMasterSynchronizer
        implements GenericMasterRecordSynchronizer {

    private final ClosingSettingRepository repository;

    @Override
    public String masterCode() {
        return PayrollClosingSettingGenericMasterContract.MASTER_CODE;
    }

    @Override
    public void synchronize(List<GenericMasterRecordCandidate> records) {
        GenericMasterRecordCandidate source = records.stream()
                .filter(GenericMasterRecordCandidate::activeFlag)
                .filter(record -> ClosingSettingDefaults.PAYROLL_SETTING_CODE.equals(
                        text(record, "SETTING_CODE")
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "有効な給与締日設定がありません。"
                ));
        ClosingSetting target = repository
                .findFirstBySettingCodeAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(
                        ClosingSettingDefaults.PAYROLL_SETTING_CODE
                )
                .orElseGet(ClosingSetting::new);
        target.setSettingCode(ClosingSettingDefaults.PAYROLL_SETTING_CODE);
        applyRule(target, source, "CLOSING", true);
        applyRule(target, source, "PAYMENT", false);
        target.setActiveFlag(true);
        repository.save(target);
    }

    private void applyRule(
            ClosingSetting target,
            GenericMasterRecordCandidate source,
            String prefix,
            boolean closing
    ) {
        DayRuleType type = DayRuleType.valueOf(text(source, prefix + "_DAY_TYPE"));
        Integer day = type == DayRuleType.END_OF_MONTH
                ? null
                : integer(source, prefix + "_DAY_VALUE");
        int monthOffset = integer(source, prefix + "_MONTH_OFFSET");
        if (closing) {
            target.setClosingDayType(type);
            target.setClosingDayValue(day);
            target.setClosingMonthOffset(monthOffset);
        } else {
            target.setPaymentDayType(type);
            target.setPaymentDayValue(day);
            target.setPaymentMonthOffset(monthOffset);
        }
    }

    private int integer(GenericMasterRecordCandidate source, String key) {
        return new BigDecimal(text(source, key)).intValueExact();
    }

    private String text(GenericMasterRecordCandidate source, String key) {
        Object value = source.values().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
