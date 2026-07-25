package com.project.backend.features.system.rule.service.executor;

import java.util.Map;

import org.apache.commons.jexl3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.context.RuleExecutionContext;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.service.validation.RuleDslSafety;
import org.apache.commons.jexl3.introspection.JexlPermissions;

@Service
public class JexlDslExecutor implements DslExecutor {

    private final JexlEngine jexl =
            new JexlBuilder()
                    .strict(true)
                    .safe(true)
                    .silent(false)
                    .permissions(JexlPermissions.RESTRICTED)
                    .create();

    @Override
    public RuleDslType supports() {
        return RuleDslType.JEXL;
    }

    @Override
    public Object execute(
            RuleExecutionContext context
    ) {
        RuleMaster rule = context.getRule();

        if (!StringUtils.hasText(rule.getDslText())) {
            throw new RuntimeException("dslText は必須です。 ruleName=" + rule.getRuleName());
        }

        RuleDslSafety.validate(rule.getDslText());

        JexlContext jexlContext = new MapContext();

        for (Map.Entry<String, Object> entry : context.getFacts().entrySet()) {
            jexlContext.set(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        JexlExpression expression =
                jexl.createExpression(rule.getDslText());

        return expression.evaluate(jexlContext);
    }
}
