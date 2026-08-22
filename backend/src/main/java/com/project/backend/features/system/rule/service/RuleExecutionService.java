package com.project.backend.features.system.rule.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.context.RuleExecutionContext;
import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.exception.RuleExecutionException;
import com.project.backend.features.system.rule.exception.RuleExecutionStage;
import com.project.backend.features.system.rule.service.builder.RuleFactBuilder;
import com.project.backend.features.system.rule.service.executor.DslExecutorDispatcher;
import com.project.backend.features.system.rule.service.loader.RuleLoader;
import com.project.backend.features.system.rule.service.validation.RuleParameterResolver;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleExecutionService {

    private static final Logger log = LoggerFactory.getLogger(
            RuleExecutionService.class
    );
    private static final Pattern SAFE_RULE_NAME = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_]{0,149}$"
    );

    private final RuleLoader ruleLoader;
    private final RuleFactBuilder factBuilder;
    private final DslExecutorDispatcher dispatcher;
    private final RuleParameterResolver parameterResolver;

    @Transactional(readOnly = true)
    public RuleExecutionResult execute(
            String ruleName,
            RuleContextRequest contextRequest
    ) {
        RuleMaster rule = executeStage(
                ruleName,
                RuleExecutionStage.RULE_LOADING,
                () -> ruleLoader.loadActive(ruleName)
        );
        Map<String, Object> parameters = executeStage(
                ruleName,
                RuleExecutionStage.PARAMETER_RESOLUTION,
                () -> parameterResolver.resolve(
                        rule,
                        toParameterMap(contextRequest)
                )
        );

        Map<String, Object> facts = executeStage(
                ruleName,
                RuleExecutionStage.FACT_BUILDING,
                () -> factBuilder.build(
                        rule,
                        RuleContextRequest.builder()
                                .parameters(parameters)
                                .build()
                )
        );

        RuleExecutionContext context =
                RuleExecutionContext.builder()
                        .rule(rule)
                        .parameters(parameters)
                        .facts(facts)
                        .build();

        Object result = executeStage(
                ruleName,
                RuleExecutionStage.DSL_EXECUTION,
                () -> dispatcher.execute(context)
        );
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

    private <T> T executeStage(
            String ruleName,
            RuleExecutionStage stage,
            Supplier<T> action
    ) {
        try {
            return action.get();
        } catch (RuleExecutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            String failureType = exception.getClass().getSimpleName();
            String ruleReference = safeRuleReference(ruleName);
            log.warn(
                    "rule execution failed: ruleName={}, stage={}, failureType={}",
                    ruleReference,
                    stage,
                    failureType
            );
            // 元例外は入力値・Fact・DSL断片を含む可能性があるため、
            // 通常ログと外部レスポンスへ伝播させない。
            throw new RuleExecutionException(
                    ruleReference,
                    stage,
                    failureType
            );
        }
    }

    private String safeRuleReference(String ruleName) {
        return ruleName != null && SAFE_RULE_NAME.matcher(ruleName).matches()
                ? ruleName
                : "<invalid>";
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
