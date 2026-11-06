package com.project.backend.features.dailyreport.service;

import com.project.backend.features.dailyreport.enums.DailyPayComponentType;

public final class DailyPayRuleGatewayKeys {

    private static final String PREFIX = "DAILY_PAY.";

    private DailyPayRuleGatewayKeys() {
    }

    public static String forComponent(DailyPayComponentType componentType) {
        return PREFIX + componentType.name();
    }

    public static DailyPayComponentType toComponent(String gatewayKey) {
        if (!supports(gatewayKey)) {
            throw new IllegalArgumentException(
                    "日報給与のGateway Keyではありません。gatewayKey="
                            + gatewayKey
            );
        }
        return DailyPayComponentType.valueOf(
                gatewayKey.substring(PREFIX.length())
        );
    }

    public static boolean supports(String gatewayKey) {
        if (gatewayKey == null || !gatewayKey.startsWith(PREFIX)) {
            return false;
        }
        try {
            DailyPayComponentType.valueOf(
                    gatewayKey.substring(PREFIX.length())
            );
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
