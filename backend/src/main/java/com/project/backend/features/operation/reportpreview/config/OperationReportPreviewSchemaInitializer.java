package com.project.backend.features.operation.reportpreview.config;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * HibernateがEntityテーブルを準備した後、ローカル検証環境へ
 * 日次プレビュー用Viewとマスターを冪等適用する。
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@ConditionalOnProperty(
        prefix = "app.report.preview.schema-init",
        name = "enabled",
        havingValue = "true"
)
public class OperationReportPreviewSchemaInitializer
        implements ApplicationRunner {

    private static final String SCHEMA_RESOURCE =
            "sql/system/report/preview/daily_preview_foundation_v1.sql";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(
                        new ClassPathResource(SCHEMA_RESOURCE)
                );
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}
