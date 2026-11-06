package com.project.backend.features.master.payrollitem.service;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public final class PayrollItemRuleGatewayKeys {

    private static final String PREFIX = "PAYROLL_ITEM.";

    private PayrollItemRuleGatewayKeys() {
    }

    public static String forMaster(
            PayrollItemTargetType targetType,
            Long masterId
    ) {
        if (targetType == null || masterId == null || masterId <= 0) {
            throw new IllegalArgumentException(
                    "給与項目Gateway KeyにはtargetTypeと正のmasterIdが必要です。"
            );
        }
        return PREFIX + targetType.name() + "." + masterId;
    }

    public static boolean supports(String gatewayKey) {
        try {
            parse(gatewayKey);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static Descriptor parse(String gatewayKey) {
        if (gatewayKey == null || !gatewayKey.startsWith(PREFIX)) {
            throw invalid(gatewayKey);
        }
        String[] parts = gatewayKey.split("\\.");
        if (parts.length != 3) {
            throw invalid(gatewayKey);
        }
        try {
            PayrollItemTargetType targetType =
                    PayrollItemTargetType.valueOf(parts[1]);
            Long masterId = Long.valueOf(parts[2]);
            if (masterId <= 0) {
                throw invalid(gatewayKey);
            }
            return new Descriptor(targetType, masterId);
        } catch (IllegalArgumentException exception) {
            throw invalid(gatewayKey);
        }
    }

    private static IllegalArgumentException invalid(String gatewayKey) {
        return new IllegalArgumentException(
                "給与項目Rule Gateway Keyが不正です。gatewayKey=" + gatewayKey
        );
    }

    public record Descriptor(
            PayrollItemTargetType targetType,
            Long masterId
    ) {
    }
}
