package com.project.backend.common.closing;

import com.project.backend.common.closing.entity.ClosingSetting;
import com.project.backend.common.dayrule.enums.DayRuleType;

public final class ClosingSettingDefaults {

    public static final String PAYROLL_SETTING_CODE = "PAYROLL";

    private ClosingSettingDefaults() {
    }

    public static ClosingSetting payroll() {
        ClosingSetting setting = new ClosingSetting();
        setting.setSettingCode(PAYROLL_SETTING_CODE);
        setting.setClosingDayType(DayRuleType.END_OF_MONTH);
        setting.setClosingDayValue(null);
        setting.setClosingMonthOffset(0);
        setting.setPaymentDayType(DayRuleType.DAY_OF_MONTH);
        setting.setPaymentDayValue(25);
        setting.setPaymentMonthOffset(1);
        setting.setActiveFlag(true);
        return setting;
    }
}
