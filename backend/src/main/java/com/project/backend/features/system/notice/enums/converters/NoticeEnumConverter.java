package com.project.backend.features.system.notice.enums.converters;

import com.project.backend.common.dayrule.enums.DayRuleType;

public class NoticeEnumConverter {

    public static DayRuleType toDayRuleType(Object value) {
        if (value == null) {
            return null;
        }

        return DayRuleType.valueOf(String.valueOf(value));
    }

}
