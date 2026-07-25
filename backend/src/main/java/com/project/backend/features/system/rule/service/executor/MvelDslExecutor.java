package com.project.backend.features.system.rule.service.executor;

import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.context.RuleExecutionContext;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.service.validation.RuleDslSafety;

@Service
public class MvelDslExecutor implements DslExecutor {

    @Override
    public RuleDslType supports() {
        return RuleDslType.MVEL;
    }

    @Override
    public Object execute(
            RuleExecutionContext context
    ) {
        RuleMaster rule = context.getRule();

        if (!StringUtils.hasText(rule.getDslText())) {
            throw new RuntimeException(
                    "dslText は必須です。 ruleName="
                            + rule.getRuleName()
            );
        }

        RuleDslSafety.validate(rule.getDslText());

        Map<String, Object> variables =
                new HashMap<>(context.getFacts());

        return MVEL.eval(
                rule.getDslText(),
                variables
        );
    }
}
