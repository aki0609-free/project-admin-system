package com.project.backend.features.operation.reportpreview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewHtmlRequest;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;

class OperationReportPreviewRowReaderServiceTest {

    private final NamedParameterJdbcTemplate jdbcTemplate =
            Mockito.mock(NamedParameterJdbcTemplate.class);
    private final OperationReportPreviewRowReaderService service =
            new OperationReportPreviewRowReaderService(jdbcTemplate);

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void usesMasterManagedTargetDateColumn() {
        when(jdbcTemplate.queryForList(anyString(), anyMap()))
                .thenReturn(List.of());
        OperationReportPreview definition = definition();

        service.readRows(
                definition,
                new OperationReportPreviewHtmlRequest(
                        OperationType.DAILY,
                        "DAILY_LABOR_COST_PREVIEW",
                        "2026-08-01",
                        null,
                        null,
                        null,
                        null
                ),
                "default"
        );

        ArgumentCaptor<String> sql =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parameters =
                ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate).queryForList(
                sql.capture(),
                parameters.capture()
        );
        assertThat(sql.getValue()).contains("and target_date = :targetDate");
        assertThat(parameters.getValue().get("targetDate"))
                .isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void rejectsUnsafeFilterColumn() {
        OperationReportPreview definition = definition();
        definition.setFilterColumnName("target_date; delete");

        assertThatThrownBy(() -> service.readRows(
                definition,
                new OperationReportPreviewHtmlRequest(
                        OperationType.DAILY,
                        "DAILY_LABOR_COST_PREVIEW",
                        "2026-08-01",
                        null,
                        null,
                        null,
                        null
                ),
                "default"
        )).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("filter_column_name");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void customerInvoicePreviewUsesCustomerSpecificClosingPeriod() {
        when(jdbcTemplate.queryForList(anyString(), anyMap()))
                .thenReturn(List.of());
        OperationReportPreview definition = new OperationReportPreview();
        definition.setOperationType(OperationType.MONTHLY);
        definition.setTableName("vw_monthly_invoice_operation_preview");
        definition.setFilterColumnName("target_month");
        definition.setOrderBy("customer_name, site_name, job_code");

        service.readRows(
                definition,
                new OperationReportPreviewHtmlRequest(
                        OperationType.MONTHLY,
                        "MONTHLY_INVOICE",
                        null,
                        "2026-09",
                        30L,
                        "2026-08-21",
                        "2026-09-20"
                ),
                "default"
        );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parameters = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate).queryForList(sql.capture(), parameters.capture());
        assertThat(sql.getValue())
                .contains("customer_id = :customerId")
                .contains("work_date between :periodFrom and :periodTo");
        assertThat(parameters.getValue())
                .containsEntry("customerId", 30L)
                .containsEntry("periodFrom", LocalDate.of(2026, 8, 21))
                .containsEntry("periodTo", LocalDate.of(2026, 9, 20));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void customerOrderFormPreviewUsesTargetMonthAndCustomer() {
        when(jdbcTemplate.queryForList(anyString(), anyMap()))
                .thenReturn(List.of());
        OperationReportPreview definition = new OperationReportPreview();
        definition.setOperationType(OperationType.MONTHLY);
        definition.setTableName("monthly_order_form_history");
        definition.setFilterColumnName("target_month");

        service.readRows(
                definition,
                new OperationReportPreviewHtmlRequest(
                        OperationType.MONTHLY,
                        "MONTHLY_ORDER_FORM",
                        null,
                        "2026-09",
                        30L,
                        "2026-08-21",
                        "2026-09-20"
                ),
                "default"
        );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parameters = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate).queryForList(sql.capture(), parameters.capture());
        assertThat(sql.getValue())
                .contains("date_format(target_month, '%Y-%m') = :targetMonth")
                .contains("customer_id = :customerId");
        assertThat(parameters.getValue())
                .containsEntry("targetMonth", "2026-09")
                .containsEntry("customerId", 30L);
    }

    @Test
    void customerScopedPreviewRejectsPartialScope() {
        OperationReportPreview definition = new OperationReportPreview();
        definition.setOperationType(OperationType.MONTHLY);
        definition.setTableName("vw_monthly_invoice_operation_preview");
        definition.setFilterColumnName("target_month");

        assertThatThrownBy(() -> service.readRows(
                definition,
                new OperationReportPreviewHtmlRequest(
                        OperationType.MONTHLY,
                        "MONTHLY_INVOICE",
                        null,
                        "2026-09",
                        30L,
                        null,
                        null
                ),
                "default"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId、periodFrom、periodTo");
    }

    private OperationReportPreview definition() {
        OperationReportPreview definition =
                new OperationReportPreview();
        definition.setOperationType(OperationType.DAILY);
        definition.setTableName("vw_daily_labor_cost_preview");
        definition.setFilterColumnName("target_date");
        definition.setOrderBy("payment_cycle, employee_code");
        return definition;
    }
}
