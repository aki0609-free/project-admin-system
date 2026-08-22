package com.project.backend.features.master.payrollitem.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 給与計算における数値変換と円単位の端数処理を統一する。
 */
@Component
public class PayrollMoneyPolicy {

    public BigDecimal toDecimal(
            Object value,
            String valueName
    ) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof BigInteger integer) {
                return new BigDecimal(integer);
            }
            if (value instanceof Byte
                    || value instanceof Short
                    || value instanceof Integer
                    || value instanceof Long) {
                return BigDecimal.valueOf(
                        ((Number) value).longValue()
                );
            }
            if (value instanceof Float
                    || value instanceof Double) {
                double decimal = ((Number) value).doubleValue();
                if (!Double.isFinite(decimal)) {
                    throw new IllegalArgumentException(
                            valueName + "が有限の数値ではありません。"
                    );
                }
                return BigDecimal.valueOf(decimal);
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }

            String text = String.valueOf(value).trim();
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException(
                        valueName + "は数値で指定してください。"
                );
            }
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    valueName + "は数値で指定してください。",
                    exception
            );
        }
    }

    public BigDecimal roundToYen(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal requireNonNegative(
            BigDecimal value,
            String valueName
    ) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(
                    valueName + "は0以上で指定してください。"
            );
        }
        return normalized;
    }
}
