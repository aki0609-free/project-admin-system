package com.project.backend.features.system.report.service.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
import com.project.backend.features.system.report.enums.ReportOutputFormat;

class ReportMasterSnapshotValidatorTest {

    private final ReportMasterValidator validator =
            new ReportMasterValidator(new ReportTemplateValidator());

    @Test
    void validate_shouldAcceptSafeSnapshotDefinition() {
        ReportMasterSaveRequest request = baseRequest();
        when(request.sourceViewName())
                .thenReturn("vw_monthly_pay_slip");
        when(request.historyTable())
                .thenReturn("monthly_pay_slip_history");
        when(request.htmlTemplateKey()).thenReturn(
                "documents/templates/reports/html/"
                        + "MONTHLY_PAY_SLIP/v1/template.html"
        );
        when(request.htmlTemplateVersion()).thenReturn(1);
        when(request.htmlTemplateHash()).thenReturn(
                "a".repeat(64)
        );

        assertThatCode(() -> validator.validate(request))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectUnsafeViewName() {
        ReportMasterSaveRequest request = baseRequest();
        when(request.sourceViewName())
                .thenReturn("vw_report;drop table employee");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("sourceViewName");
    }

    @Test
    void validate_shouldRejectTemplateTraversal() {
        ReportMasterSaveRequest request = baseRequest();
        when(request.htmlTemplateKey())
                .thenReturn("../secret/template.html");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("htmlTemplateKey");
    }

    @Test
    void validate_shouldRejectUnsafeReportCode() {
        ReportMasterSaveRequest request = baseRequest();
        when(request.reportCode()).thenReturn("../PAY_SLIP");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("reportCode");
    }

    @Test
    void validate_shouldRejectUnsafeDynamicTableName() {
        ReportMasterSaveRequest request = baseRequest();
        when(request.workTable()).thenReturn("pay_slip;drop_table");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("workTable");
    }

    @Test
    void validate_shouldRejectTemplateExtensionThatDoesNotMatchOutput() {
        ReportMasterSaveRequest request = baseRequest();
        when(request.templateFileName()).thenReturn("pay-slip.xlsx");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(".jrxml");
    }

    @Test
    void validate_shouldAllowTemplateDrivenExcelOutput() {
        ReportMasterSaveRequest request = baseRequest();
        when(request.outputFormat()).thenReturn(ReportOutputFormat.EXCEL);
        when(request.templateFileName()).thenReturn("labor-cost.xlsx");

        assertThatCode(() -> validator.validate(request))
                .doesNotThrowAnyException();
    }

    private ReportMasterSaveRequest baseRequest() {
        ReportMasterSaveRequest request =
                mock(ReportMasterSaveRequest.class);
        when(request.reportCode()).thenReturn("MONTHLY_PAY_SLIP");
        when(request.reportName()).thenReturn("月次給与明細");
        when(request.workTable()).thenReturn("monthly_pay_slip");
        when(request.outputFormat()).thenReturn(ReportOutputFormat.PDF);
        when(request.templateFileName()).thenReturn("monthly_pay_slip.jrxml");
        when(request.htmlTemplateVersion()).thenReturn(null);
        return request;
    }
}
