package com.project.backend.features.operation.reportpreview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewHtmlRequest;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewColumnRepository;
import com.project.backend.features.system.report.service.core.ReportHtmlTemplateRenderer;
import com.project.backend.features.system.report.service.loader.ReportHtmlTemplateLoader;

class OperationReportPreviewHtmlServiceTest {

    private final OperationReportPreviewService previewService =
            mock(OperationReportPreviewService.class);
    private final OperationReportPreviewColumnRepository columnRepository =
            mock(OperationReportPreviewColumnRepository.class);
    private final OperationReportPreviewRowReaderService rowReaderService =
            mock(OperationReportPreviewRowReaderService.class);
    private final ReportHtmlTemplateLoader templateLoader =
            mock(ReportHtmlTemplateLoader.class);
    private final ReportHtmlTemplateRenderer templateRenderer =
            mock(ReportHtmlTemplateRenderer.class);
    private final OperationReportPreviewHtmlService service =
            new OperationReportPreviewHtmlService(
                    previewService,
                    columnRepository,
                    rowReaderService,
                    templateLoader,
                    templateRenderer
            );

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void acceptsDefaultAsExplicitTenantId() {
        OperationReportPreview definition = definition();
        OperationReportPreviewHtmlRequest request = request();
        when(previewService.findDefinition(
                OperationType.DAILY,
                "DAILY_SAMPLE"
        )).thenReturn(definition);
        when(columnRepository
                .findByPreviewIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        1L
                )).thenReturn(List.of());
        when(rowReaderService.readRows(
                definition,
                request,
                "default"
        )).thenReturn(List.of());
        when(templateLoader.loadOrDefault(definition))
                .thenReturn("<html></html>");
        when(templateRenderer.render(anyString(), anyMap()))
                .thenReturn("rendered");
        TenantContext.setTenantId("default");

        assertThat(service.renderHtml(request)).isEqualTo("rendered");
        verify(rowReaderService).readRows(
                definition,
                request,
                "default"
        );
    }

    @Test
    void rejectsMissingTenantContextInsteadOfFallingBackToDefault() {
        OperationReportPreview definition = definition();
        OperationReportPreviewHtmlRequest request = request();
        when(previewService.findDefinition(any(), anyString()))
                .thenReturn(definition);
        when(columnRepository
                .findByPreviewIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        eq(1L)
                )).thenReturn(List.of());

        assertThatThrownBy(() -> service.renderHtml(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenantIdを取得できません");
    }

    private OperationReportPreview definition() {
        OperationReportPreview definition = new OperationReportPreview();
        definition.setId(1L);
        definition.setOperationType(OperationType.DAILY);
        definition.setReportCode("DAILY_SAMPLE");
        definition.setOutputType(OperationReportOutputType.PDF);
        return definition;
    }

    private OperationReportPreviewHtmlRequest request() {
        return new OperationReportPreviewHtmlRequest(
                OperationType.DAILY,
                "DAILY_SAMPLE",
                "2026-09-06",
                null,
                null,
                null,
                null
        );
    }
}
