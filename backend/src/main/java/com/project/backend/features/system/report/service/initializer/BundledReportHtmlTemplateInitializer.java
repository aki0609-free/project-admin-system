package com.project.backend.features.system.report.service.initializer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.service.builder.ReportHtmlTemplateKeyBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.report.init",
        name = "enabled",
        havingValue = "true"
)
public class BundledReportHtmlTemplateInitializer
        implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            BundledReportHtmlTemplateInitializer.class
    );

    private static final List<TemplateDefinition> TEMPLATES = List.of(
            new TemplateDefinition(
                    "DAILY_LABOR_COST_PREVIEW",
                    1,
                    "daily_labor_cost.html"
            ),
            new TemplateDefinition(
                    "DAILY_PAYMENT_PREPARATION",
                    1,
                    "daily_payment_preparation.html"
            ),
            new TemplateDefinition(
                    "DAILY_PAY_SLIP",
                    2,
                    "daily_pay_slip.html"
            ),
            new TemplateDefinition(
                    "MONTHLY_PAY_SLIP",
                    2,
                    "monthly_pay_slip.html"
            )
    );

    private final StorageService storageService;
    private final ReportHtmlTemplateKeyBuilder keyBuilder;

    @Value("${app.report.init.overwrite-existing:false}")
    private boolean overwriteExisting;

    @Override
    public void run(ApplicationArguments args) {
        TEMPLATES.forEach(this::initializeIfMissing);
    }

    private void initializeIfMissing(TemplateDefinition definition) {
        String key = keyBuilder.build(
                definition.reportCode(),
                definition.version()
        );
        if (storageService.exists(key) && !overwriteExisting) {
            log.info("HTML report template already exists. key={}", key);
            return;
        }

        ClassPathResource resource = new ClassPathResource(
                "templates/operation/reportpreview/"
                        + definition.classpathFileName()
        );
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Bundled HTML report template is missing. fileName="
                            + definition.classpathFileName()
            );
        }

        try (var inputStream = resource.getInputStream()) {
            storageService.save(
                    key,
                    inputStream,
                    resource.contentLength(),
                    "text/html; charset=UTF-8"
            );
            log.info(
                    "Bundled HTML report template {}. key={}",
                    overwriteExisting ? "synchronized" : "initialized",
                    key
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Bundled HTML report template initialization failed. key="
                            + key,
                    e
            );
        }
    }

    private record TemplateDefinition(
            String reportCode,
            int version,
            String classpathFileName
    ) {
    }
}
