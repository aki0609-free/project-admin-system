package com.project.backend.features.system.rule.service.executor;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.context.RuleExecutionContext;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleDslType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JavaBeanDslExecutor implements DslExecutor {

    private final ApplicationContext applicationContext;
    private final RulesEngine rulesEngine;

    @Override
    public RuleDslType supports() {
        return RuleDslType.JAVA_BEAN;
    }

    @SuppressWarnings("null")
    @Override
    public Object execute(
            RuleExecutionContext context
    ) {
        RuleMaster rule = context.getRule();

        if (!StringUtils.hasText(rule.getRuleBeanName())) {
            throw new RuntimeException("JAVA_BEAN の場合 ruleBeanName は必須です。");
        }

        Object bean =
                applicationContext.getBean(rule.getRuleBeanName());

        if (!(bean instanceof Rule easyRule)) {
            throw new RuntimeException(
                    "ruleBeanName のBeanが Easy Rules Rule ではありません。 beanName="
                            + rule.getRuleBeanName()
            );
        }

        Facts facts = new Facts();

        context.getFacts().forEach(facts::put);

        Rules rules = new Rules();
        rules.register(easyRule);

        rulesEngine.fire(
                rules,
                facts
        );

        context.getFacts().putAll(facts.asMap());

        return facts.get(rule.getResultFactKey());
    }
}