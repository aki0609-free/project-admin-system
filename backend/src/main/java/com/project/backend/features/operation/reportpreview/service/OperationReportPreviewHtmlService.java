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

                String tenantId = requireTenantId();

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

        /**
         * tenantIdの値としての"default"は正式なtenantとして許可する。
         * 一方、認証/Context設定そのものが欠落したrequestをdefaultへ
         * 暗黙変換するとtenant境界を誤るため、明示的に拒否する。
         */
        private String requireTenantId() {
                String tenantId = TenantContext.getTenantId();
                if (tenantId == null || tenantId.isBlank()) {
                        throw new IllegalStateException(
                                        "帳票プレビューのtenantIdを取得できません。"
                        );
                }
                return tenantId;
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
                        case "monthly_invoice_history_id" -> "請求書履歴ID";
                        case "closing_version" -> "締めVersion";
                        case "customer_id" -> "顧客ID";
                        case "order_number" -> "注文番号";
                        case "order_date" -> "注文日";
                        case "contract_from" -> "契約開始日";
                        case "contract_to" -> "契約終了日";
                        case "subject_text" -> "件名";
                        case "subcontractor_name" -> "下請負人名";
                        case "subcontractor_postal_code" -> "下請負人郵便番号";
                        case "subcontractor_address" -> "下請負人住所";
                        case "prime_contractor_name" -> "元請負人名";
                        case "prime_contractor_postal_code" -> "元請負人郵便番号";
                        case "prime_contractor_address" -> "元請負人住所";
                        case "show_prime_contractor" -> "元請負人表示";
                        case "tax_rate" -> "税率";
                        case "construction_price" -> "工事価格";
                        case "tax_amount" -> "消費税額";
                        case "contract_amount" -> "請負代金額";
                        case "source_execution_id" -> "実行ID";
                        case "fixed_at" -> "確定日時";
                        default -> columnName;
                };
        }
}
