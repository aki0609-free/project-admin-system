package com.project.backend.features.master.payrollitem.service.validation;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.system.rule.enums.RuleType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PayrollItemMasterValidator {

    private static final Pattern MASTER_CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");

    private final RuleMasterRepository ruleMasterRepository;

    public void validate(
            String code,
            String name,
            String calculationType,
            String ruleName,
            Integer defaultAmount,
            Boolean allowManualInput,
            Integer minAmount,
            Integer maxAmount,
            Integer displayOrder,
            RuleType expectedRuleType
    ) {
        String normalizedCode = normalizeCode(code);

        if (!MASTER_CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException(
                    "マスターコードは英大文字で始まる50文字以内の英大文字・数字・_で入力してください。"
            );
        }
        if (!StringUtils.hasText(name) || name.trim().length() > 100) {
            throw new IllegalArgumentException("名称は100文字以内で入力してください。");
        }
        if (!StringUtils.hasText(calculationType)) {
            throw new IllegalArgumentException("計算区分は必須です。");
        }

        validateNonNegative("初期金額", defaultAmount);
        validateNonNegative("下限金額", minAmount);
        validateNonNegative("上限金額", maxAmount);
        validateNonNegative("表示順", displayOrder);

        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            throw new IllegalArgumentException("下限金額は上限金額以下にしてください。");
        }

        switch (calculationType) {
            case "AUTO" -> validateActiveRule(ruleName, expectedRuleType);
            case "FIXED" -> {
                if (defaultAmount == null) {
                    throw new IllegalArgumentException("固定計算では初期金額が必須です。");
                }
            }
            case "MANUAL" -> {
                if (!Boolean.TRUE.equals(allowManualInput)) {
                    throw new IllegalArgumentException("手入力計算では手入力許可を有効にしてください。");
                }
            }
            default -> throw new IllegalArgumentException(
                    "未対応の計算区分です。calculationType=" + calculationType
            );
        }
    }

    public String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    public String normalizeRuleName(String calculationType, String ruleName) {
        if (!"AUTO".equals(calculationType)) {
            return null;
        }
        return ruleName == null ? null : ruleName.trim();
    }

    private void validateActiveRule(String ruleName, RuleType expectedRuleType) {
        if (!StringUtils.hasText(ruleName)) {
            throw new IllegalArgumentException("自動計算ではRuleの選択が必須です。");
        }

        String tenantId = requireTenantId();
        var rule = ruleMasterRepository
                .findByTenantIdAndRuleNameAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId,
                        ruleName.trim()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "選択されたRuleが存在しないか無効です。ruleName=" + ruleName.trim()
                ));

        if (expectedRuleType != null
                && rule.getRuleType() != expectedRuleType
                && rule.getRuleType() != RuleType.GENERAL) {
            throw new IllegalArgumentException(
                    "給与項目の種別とRule種別が一致しません。expected="
                            + expectedRuleType
                            + ", actual="
                            + rule.getRuleType()
            );
        }
    }

    private void validateNonNegative(String label, Integer value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(label + "は0以上で入力してください。");
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("TenantContext に tenantId が設定されていません。");
        }
        return tenantId;
    }
}
