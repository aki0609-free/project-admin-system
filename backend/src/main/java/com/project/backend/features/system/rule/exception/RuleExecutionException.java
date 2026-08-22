package com.project.backend.features.system.rule.exception;

import lombok.Getter;

@Getter
public class RuleExecutionException extends RuntimeException {

    private final String ruleName;
    private final RuleExecutionStage stage;
    private final String failureType;

    public RuleExecutionException(
            String ruleName,
            RuleExecutionStage stage,
            String failureType
    ) {
        super("Ruleの実行に失敗しました。追跡IDを管理者へ連絡してください。");
        this.ruleName = ruleName;
        this.stage = stage;
        this.failureType = failureType;
    }
}
