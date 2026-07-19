package com.project.backend.features.system.rule.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.project.backend.features.system.rule.context.RuleExecutionContext;
import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.service.builder.RuleFactBuilder;
import com.project.backend.features.system.rule.service.executor.DslExecutorDispatcher;
import com.project.backend.features.system.rule.service.loader.RuleLoader;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class RuleExecutionService {

    private final RuleLoader ruleLoader;
    private final RuleFactBuilder factBuilder;
    private final DslExecutorDispatcher dispatcher;

    @Transactional(readOnly = true)
    public RuleExecutionResult execute(
            String ruleName,
            RuleContextRequest contextRequest
    ) {
        RuleMaster rule = ruleLoader.loadActive(ruleName);

        Map<String, Object> facts =
                factBuilder.build(
                        rule,
                        contextRequest
                );

        RuleExecutionContext context =
                RuleExecutionContext.builder()
                        .rule(rule)
                        .parameters(toParameterMap(contextRequest))
                        .facts(facts)
                        .build();

        Object result = dispatcher.execute(context);
        String resultFactKey =
                StringUtils.hasText(rule.getResultFactKey())
                        ? rule.getResultFactKey()
                        : "result";

        facts.put(resultFactKey, result);

        return RuleExecutionResult.builder()
                .ruleName(ruleName)
                .executed(true)
                .result(result)
                .facts(new LinkedHashMap<>(facts))
                .message("Ruleを実行しました。")
                .build();
    }

    private Map<String, Object> toParameterMap(
            RuleContextRequest request
    ) {
        if (request == null || request.parameters() == null) {
            return Map.of();
        }
        return request.parameters();
    }

}