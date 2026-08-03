package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalogColumn;
import com.project.backend.features.system.excelbook.entity.ExcelBookVariableMapping;

class ExcelBookDataSourceRowQueryServiceTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private ExcelBookDataSourceRowQueryService service;
    private ExcelBookDataSourceCatalog catalog;
    private ExcelBookVariableMapping mapping;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        service = new ExcelBookDataSourceRowQueryService(
                jdbcTemplate
        );
        TenantContext.setTenantId("tenant-a");

        catalog = new ExcelBookDataSourceCatalog();
        catalog.setPhysicalName("vw_monthly_employee_ledger");
        catalog.setWhereClauseTemplate(
                "tenant_id = :tenantId AND target_month = :targetMonth"
        );
        catalog.setTenantScopedFlag(true);
        catalog.setMaxRows(1000);
        catalog.getColumns().add(column("employee_code", 1));
        catalog.getColumns().add(column("employee_name", 2));

        mapping = new ExcelBookVariableMapping();
        mapping.setSourceColumn("employee_name");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findRows_shouldUseCatalogViewAndAllowedParameters() {
        when(jdbcTemplate.queryForList(
                contains("vw_monthly_employee_ledger"),
                anyMap()
        )).thenReturn(List.of(Map.of(
                "employee_name", "山田 太郎"
        )));

        var result = service.findRows(
                catalog,
                List.of(mapping),
                "2026-07"
        );

        assertThat(result).hasSize(1);
        ArgumentCaptor<Map> parameters =
                ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate).queryForList(
                contains(
                        "tenant_id = :tenantId AND target_month = :targetMonth"
                ),
                parameters.capture()
        );
        assertThat((Map<String, Object>) parameters.getValue())
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("targetMonth", "2026-07");
    }

    @Test
    void findRows_shouldRejectDangerousWhereClause() {
        catalog.setWhereClauseTemplate(
                "tenant_id = :tenantId; DROP TABLE employee"
        );

        assertThatThrownBy(() -> service.findRows(
                catalog,
                List.of(mapping),
                "2026-07"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WHERE句");
    }

    private ExcelBookDataSourceCatalogColumn column(
            String name,
            int orderNo
    ) {
        ExcelBookDataSourceCatalogColumn column =
                new ExcelBookDataSourceCatalogColumn();
        column.setColumnName(name);
        column.setOrderNo(orderNo);
        column.setActiveFlag(true);
        return column;
    }
}
