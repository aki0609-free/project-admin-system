package com.project.backend.common.closing.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.common.closing.entity.ClosingSetting;
import com.project.backend.common.closing.repository.ClosingSettingRepository;
import com.project.backend.common.dayrule.dto.DayRule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClosingSettingQueryService {

    private static final String PAYROLL_SETTING_CODE = "PAYROLL";

    private final ClosingSettingRepository repository;

    public ClosingSetting findPayrollSetting() {
        return repository
                .findFirstBySettingCodeAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(
                        PAYROLL_SETTING_CODE
                )
                .orElseThrow(() -> new RuntimeException("給与締日設定が見つかりません。"));
    }

    /**
     * 給与締日ルール取得
     */
    public DayRule getPayrollClosingDayRule() {
        ClosingSetting setting = findPayrollSetting();

        if (setting.getClosingDayType() == null) {
            throw new RuntimeException("給与締日設定が不正です。");
        }

        return DayRule.builder()
                .type(setting.getClosingDayType())
                .value(setting.getClosingDayValue())
                .monthOffset(setting.getClosingMonthOffset())
                .build();
    }

    /**
     * 給与支払日ルール取得
     */
    public DayRule getPayrollPaymentDayRule() {
        ClosingSetting setting = findPayrollSetting();

        if (setting.getPaymentDayType() == null) {
            throw new RuntimeException("給与支払日設定が不正です。");
        }

        return DayRule.builder()
                .type(setting.getPaymentDayType())
                .value(setting.getPaymentDayValue())
                .monthOffset(setting.getPaymentMonthOffset())
                .build();
    }
}