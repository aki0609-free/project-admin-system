package com.project.backend.features.system.rule.service.executor;

import com.project.backend.features.system.rule.context.RuleExecutionContext;
import com.project.backend.features.system.rule.enums.RuleDslType;

public interface DslExecutor {

    RuleDslType supports();

    Object execute(RuleExecutionContext context);
}