package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.PreviewReportSettingSaveRequest;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;
import com.project.backend.features.system.report.service.builder.ReportHtmlTemplateKeyBuilder;

class PreviewReportSettingServiceTest {

    private final OperationReportPreviewRepository repository =
            mock(OperationReportPreviewRepository.class);
    private final ReportHtmlTemplateKeyBuilder keyBuilder =
            mock(ReportHtmlTemplateKeyBuilder.class);
    private final StorageService storageService = mock(StorageService.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final PreviewReportSettingService service =
            new PreviewReportSettingService(
                    repository,
                    keyBuilder,
                    storageService,
                    jdbcTemplate
            );

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("default");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void save_shouldCreateHtmlPreviewAtVersionOne() {
        String key = "documents/templates/reports/html/TEST_REPORT/v1/template.html";
        when(repository.existsByTenantIdAndOperationTypeAndReportCodeAndDeletedAtIsNull(
                "default", OperationType.DAILY, "TEST_REPORT"
        )).thenReturn(false);
        when(keyBuilder.build("TEST_REPORT", 1)).thenReturn(key);
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Integer.class), eq("vw_test_preview")
        )).thenReturn(1);
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Integer.class),
                eq("vw_test_preview"), eq("target_date")
        )).thenReturn(1);
        when(repository.save(any(OperationReportPreview.class)))
                .thenAnswer(invocation -> {
                    OperationReportPreview value = invocation.getArgument(0);
                    value.setId(10L);
                    return value;
                });
        when(storageService.exists(key)).thenReturn(true);

        var result = service.save(
                request(),
                new MockMultipartFile(
                        "template",
                        "test.html",
                        "text/html",
                        "<html><body th:each=\"row : ${rows}\"></body></html>"
                                .getBytes()
                )
        );

        ArgumentCaptor<OperationReportPreview> definition =
                ArgumentCaptor.forClass(OperationReportPreview.class);
        verify(repository).save(definition.capture());
        verify(storageService).save(
                eq(key),
                any(InputStream.class),
                any(Long.class),
                eq("text/html; charset=UTF-8")
        );

        assertThat(definition.getValue().getHtmlTemplateVersion()).isOne();
        assertThat(definition.getValue().getHtmlTemplateHash()).hasSize(64);
        assertThat(definition.getValue().getColumns()).isEmpty();
        assertThat(result.templateExists()).isTrue();
        assertThat(result.htmlTemplateVersion()).isOne();
    }

    private PreviewReportSettingSaveRequest request() {
        return new PreviewReportSettingSaveRequest(
                null,
                OperationType.DAILY,
                "TEST_REPORT",
                "テスト帳票",
                "vw_test_preview",
                "target_date",
                null,
                "employee_code",
                10,
                OperationReportOutputType.HTML_PREVIEW,
                true
        );
    }
}
