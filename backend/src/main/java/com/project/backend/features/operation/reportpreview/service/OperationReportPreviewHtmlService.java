package com.project.backend.features.operation.reportpreview.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewHtmlRequest;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreviewColumn;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewColumnRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationReportPreviewHtmlService {

        private static final String DEFAULT_TENANT_ID = "default";

        private final OperationReportPreviewService previewService;
        private final OperationReportPreviewColumnRepository columnRepository;
        private final OperationReportPreviewRowReaderService rowReaderService;
        private final TemplateEngine templateEngine;

        public String renderHtml(OperationReportPreviewHtmlRequest request) {
                OperationReportPreview definition = previewService.findDefinition(
                                request.operationType(),
                                request.reportCode());

                List<OperationReportPreviewColumn> columns = columnRepository
                                .findByPreviewIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                                                definition.getId());

                String tenantId = TenantContext.getTenantId();

                if (tenantId == null || tenantId.isBlank()) {
                        tenantId = DEFAULT_TENANT_ID;
                }

                List<Map<String, Object>> rows = rowReaderService.readRows(
                                definition,
                                request,
                                tenantId);

                Context context = new Context();
                context.setVariable("definition", definition);
                context.setVariable("columns", columns);
                context.setVariable("rows", rows);
                context.setVariable("request", request);

                return templateEngine.process(
                                "operation/reportpreview/" + definition.getTemplateName(),
                                context);
        }
}