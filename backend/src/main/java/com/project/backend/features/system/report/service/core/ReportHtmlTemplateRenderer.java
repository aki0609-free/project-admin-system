package com.project.backend.features.system.report.service.core;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@Service
public class ReportHtmlTemplateRenderer {

    private final TemplateEngine templateEngine;

    public ReportHtmlTemplateRenderer() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        this.templateEngine = engine;
    }

    public String render(
            String templateSource,
            Map<String, Object> variables
    ) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateSource, context);
    }
}
