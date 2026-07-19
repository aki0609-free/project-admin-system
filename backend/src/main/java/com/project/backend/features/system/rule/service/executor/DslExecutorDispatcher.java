package com.project.backend.features.system.rule.service.executor;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.rule.context.RuleExecutionContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DslExecutorDispatcher {

    private final List<DslExecutor> executors;

    public Object execute(
            RuleExecutionContext context
    ) {
        return executors.stream()
                .filter(executor -> executor.supports() == context.getRule().getDslType())
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "未対応のdslTypeです。 dslType="
                                        + context.getRule().getDslType()
                        )
                )
                .execute(context);
    }
}