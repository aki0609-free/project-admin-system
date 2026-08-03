package com.project.backend.features.operation.reportpreview.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewHtmlRequest;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreviewColumn;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewColumnRepository;
import com.project.backend.features.system.report.service.core.ReportHtmlTemplateRenderer;
import com.project.backend.features.system.report.service.loader.ReportHtmlTemplateLoader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationReportPreviewHtmlService {

        private static final String DEFAULT_TENANT_ID = "default";
        private static final Set<String> TECHNICAL_COLUMNS = Set.of(
                        "id", "tenant_id", "created_at", "updated_at",
                        "deleted_at", "execution_id", "slip_key",
                        "business_key", "recipient_key", "recipient_name",
                        "recipient_email", "mail_type", "mail_template_key");

        private final OperationReportPreviewService previewService;
        private final OperationReportPreviewColumnRepository columnRepository;
        private final OperationReportPreviewRowReaderService rowReaderService;
        private final ReportHtmlTemplateLoader templateLoader;
        private final ReportHtmlTemplateRenderer templateRenderer;

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

                List<OperationReportPreviewColumn> effectiveColumns =
                                resolveColumns(columns, rows);

                String templateSource = requiresDedicatedTemplate(definition)
                                ? templateLoader.load(definition)
                                : templateLoader.loadOrDefault(definition);

                return templateRenderer.render(
                                templateSource,
                                Map.of(
                                                "definition", definition,
                                                "columns", effectiveColumns,
                                                "rows", rows,
                                                "request", request));
        }

        private boolean requiresDedicatedTemplate(
                        OperationReportPreview definition) {
                return definition.getOutputType()
                                == OperationReportOutputType.HTML_PREVIEW
                                || definition.getOutputType()
                                == OperationReportOutputType.HTML_PRINT;
        }

        private List<OperationReportPreviewColumn> resolveColumns(
                        List<OperationReportPreviewColumn> configured,
                        List<Map<String, Object>> rows) {
                if (!configured.isEmpty() || rows.isEmpty()) {
                        return configured;
                }

                List<OperationReportPreviewColumn> inferred = new ArrayList<>();
                int order = 1;
                for (String columnName : rows.getFirst().keySet()) {
                        if (TECHNICAL_COLUMNS.contains(columnName)) {
                                continue;
                        }
                        OperationReportPreviewColumn column =
                                        new OperationReportPreviewColumn();
                        column.setColumnName(columnName);
                        column.setPreviewName(resolvePreviewName(columnName));
                        column.setDisplayOrder(order++);
                        column.setActiveFlag(true);
                        inferred.add(column);
                }
                return inferred;
        }

        private String resolvePreviewName(String columnName) {
                return switch (columnName) {
                        case "target_date", "work_date", "payment_date" -> "対象日";
                        case "target_month" -> "対象月";
                        case "employee_code" -> "従業員コード";
                        case "employee_name" -> "従業員名";
                        case "customer_name" -> "顧客名";
                        case "site_name" -> "現場名";
                        case "work_description" -> "作業内容";
                        case "distance_from_company_km" -> "距離(km)";
                        case "vehicle_count" -> "配車台数";
                        default -> columnName;
                };
        }
}
