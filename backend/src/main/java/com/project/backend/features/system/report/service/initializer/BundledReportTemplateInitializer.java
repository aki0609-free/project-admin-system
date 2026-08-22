package com.project.backend.features.system.report.service.initializer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.service.builder.ReportTemplateKeyBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.report.init",
        name = "enabled",
        havingValue = "true"
)
public class BundledReportTemplateInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            BundledReportTemplateInitializer.class
    );

    private static final List<String> BUNDLED_TEMPLATES = List.of(
            "monthly_pay_slip.jrxml",
            "daily_pay_slip.jrxml",
            "monthly_invoice_pattern_1.jrxml",
            "monthly_invoice_pattern_2.jrxml",
            "monthly_invoice_pattern_3.jrxml",
            "monthly_order_form.jrxml",
            "daily_work_order.jrxml",
            "envelope_naga3.jrxml",
            "envelope_kaku2.jrxml",
            "monthly_labor_cost_list.xlsx"
    );

    private final StorageService storageService;
    private final ReportTemplateKeyBuilder keyBuilder;

    @Override
    public void run(ApplicationArguments args) {
        for (String fileName : BUNDLED_TEMPLATES) {
            initializeIfMissing(fileName);
        }
    }

    private void initializeIfMissing(String fileName) {
        String storageKey = keyBuilder.build(fileName);

        if (storageService.exists(storageKey)) {
            log.info(
                    "Bundled report template already exists. key={}",
                    storageKey
            );
            return;
        }

        ClassPathResource resource = new ClassPathResource(
                "reports/" + fileName
        );

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Bundled report template is missing. fileName="
                            + fileName
            );
        }

        try (var inputStream = resource.getInputStream()) {
            storageService.save(
                    storageKey,
                    inputStream,
                    resource.contentLength(),
                    contentType(fileName)
            );
            log.info(
                    "Bundled report template initialized. key={}",
                    storageKey
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Bundled report template initialization failed. key="
                            + storageKey,
                    e
            );
        }
    }

    private String contentType(String fileName) {
        if (fileName.toLowerCase().endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        return "application/xml";
    }
}
