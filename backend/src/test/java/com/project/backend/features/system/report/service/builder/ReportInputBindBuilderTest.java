package com.project.backend.features.system.report.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportParam;
import com.project.backend.features.system.report.enums.ReportParamType;
import com.project.backend.features.system.report.service.converter.ReportParamValueConverter;

class ReportInputBindBuilderTest {

    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");

    private final ReportInputBindBuilder builder = new ReportInputBindBuilder(
            new ReportParamValueConverter(),
            Clock.fixed(NOW, ZoneId.of("Asia/Tokyo"))
    );

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void buildRowsAddsTenantAndAuditColumnsToDynamicInput() {
        TenantContext.setTenantId("tenant-a");

        ReportParam targetDate = new ReportParam();
        targetDate.setParamName("targetDate");
        targetDate.setInputColumnName("target_date");
        targetDate.setParamType(ReportParamType.DATE);
        targetDate.setRequiredFlag(true);
        targetDate.setMultipleFlag(false);
        targetDate.setActiveFlag(true);

        List<Map<String, Object>> rows = builder.buildRows(
                new ReportMaster(),
                "execution-1",
                Map.of("targetDate", "2026-08-03"),
                List.of(targetDate)
        );

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("execution_id")).isEqualTo("execution-1");
            assertThat(row.get("tenant_id")).isEqualTo("tenant-a");
            assertThat(row.get("created_at"))
                    .isEqualTo(LocalDateTime.of(2026, 8, 2, 12, 0));
            assertThat(row.get("updated_at"))
                    .isEqualTo(LocalDateTime.of(2026, 8, 2, 12, 0));
            assertThat(row.get("target_date"))
                    .isEqualTo(LocalDate.of(2026, 8, 3));
        });
    }
}
