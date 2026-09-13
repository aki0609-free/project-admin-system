package com.project.backend.features.admin.business.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.PreviewReportSettingResponse;
import com.project.backend.features.admin.business.dto.PreviewReportSettingSaveRequest;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;
import com.project.backend.features.system.report.service.builder.ReportHtmlTemplateKeyBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreviewReportSettingService {

    private static final int TEMPLATE_VERSION = 1;
    private static final long MAX_TEMPLATE_SIZE = 1024L * 1024L;

    private final OperationReportPreviewRepository repository;
    private final ReportHtmlTemplateKeyBuilder templateKeyBuilder;
    private final StorageService storageService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<PreviewReportSettingResponse> findAll() {
        String tenantId = requireTenantId();
        return repository
                .findByTenantIdAndDeletedAtIsNullOrderByOperationTypeAscDisplayOrderAscIdAsc(
                        tenantId)
                .stream()
                .filter(this::isHtmlPreview)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PreviewReportSettingResponse save(
            PreviewReportSettingSaveRequest request,
            MultipartFile template
    ) {
        String tenantId = requireTenantId();
        validateOutputType(request.outputType());
        validateSource(request.tableName(), resolveFilterColumn(request));

        OperationReportPreview definition = request.id() == null
                ? createNew(request, tenantId)
                : findExisting(request.id(), tenantId);

        if (definition.getId() != null
                && (!definition.getReportCode().equals(request.reportCode())
                || definition.getOperationType() != request.operationType())) {
            throw new IllegalArgumentException(
                    "登録済み定義の帳票コードと処理区分は変更できません。"
            );
        }

        String templateKey = templateKeyBuilder.build(
                request.reportCode(), TEMPLATE_VERSION
        );
        applyRequest(definition, request, templateKey);

        if (template != null && !template.isEmpty()) {
            byte[] templateBytes = validateTemplate(template);
            storageService.save(
                    templateKey,
                    new ByteArrayInputStream(templateBytes),
                    templateBytes.length,
                    "text/html; charset=UTF-8"
            );
            definition.setHtmlTemplateHash(sha256(templateBytes));
        } else if (definition.getId() == null
                || !storageService.exists(templateKey)) {
            throw new IllegalArgumentException(
                    "新しいプレビュー帳票にはHTMLテンプレートが必要です。"
            );
        }

        return toResponse(repository.save(definition));
    }

    private OperationReportPreview createNew(
            PreviewReportSettingSaveRequest request,
            String tenantId
    ) {
        if (repository.existsByTenantIdAndOperationTypeAndReportCodeAndDeletedAtIsNull(
                tenantId, request.operationType(), request.reportCode())) {
            throw new IllegalArgumentException(
                    "同じ処理区分と帳票コードの定義が既に存在します。"
            );
        }
        OperationReportPreview definition = new OperationReportPreview();
        definition.setTenantId(tenantId);
        return definition;
    }

    private OperationReportPreview findExisting(Long id, String tenantId) {
        return repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "プレビュー帳票定義が見つかりません。 id=" + id
                ));
    }

    private void applyRequest(
            OperationReportPreview definition,
            PreviewReportSettingSaveRequest request,
            String templateKey
    ) {
        definition.setOperationType(request.operationType());
        definition.setReportCode(request.reportCode());
        definition.setReportName(request.reportName().trim());
        definition.setTableName(request.tableName().trim());
        definition.setFilterColumnName(trimToNull(request.filterColumnName()));
        definition.setTargetParamName(trimToNull(request.targetParamName()));
        definition.setOrderBy(trimToNull(request.orderBy()));
        definition.setDisplayOrder(request.displayOrder());
        definition.setOutputType(request.outputType());
        definition.setActiveFlag(request.activeFlag());
        definition.setTemplateName(request.reportCode() + ".html");
        definition.setHtmlTemplateKey(templateKey);
        definition.setHtmlTemplateVersion(TEMPLATE_VERSION);
        definition.setJobCode(null);
    }

    private byte[] validateTemplate(MultipartFile template) {
        String fileName = template.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".html")) {
            throw new IllegalArgumentException("HTMLファイルを選択してください。");
        }
        if (template.getSize() <= 0 || template.getSize() > MAX_TEMPLATE_SIZE) {
            throw new IllegalArgumentException(
                    "HTMLテンプレートは1MB以内で指定してください。"
            );
        }
        try {
            byte[] bytes = template.getBytes();
            String source = new String(bytes, StandardCharsets.UTF_8);
            if (!source.toLowerCase().contains("<html")) {
                throw new IllegalArgumentException(
                        "HTML文書として認識できないテンプレートです。"
                );
            }
            return bytes;
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException(
                    "HTMLテンプレートを読み込めませんでした。", e
            );
        }
    }

    private void validateSource(String tableName, String filterColumn) {
        Integer tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = ?
                """, Integer.class, tableName);
        if (tableCount == null || tableCount == 0) {
            throw new IllegalArgumentException(
                    "指定したデータ元View/Tableが存在しません: " + tableName
            );
        }

        Integer columnCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tableName, filterColumn);
        if (columnCount == null || columnCount == 0) {
            throw new IllegalArgumentException(
                    "指定した絞込列がデータ元に存在しません: " + filterColumn
            );
        }
    }

    private String resolveFilterColumn(PreviewReportSettingSaveRequest request) {
        if (StringUtils.hasText(request.filterColumnName())) {
            return request.filterColumnName().trim();
        }
        return switch (request.operationType()) {
            case MONTHLY -> "target_month";
            case DAILY -> "payment_date";
            default -> "target_date";
        };
    }

    private void validateOutputType(OperationReportOutputType outputType) {
        if (outputType != OperationReportOutputType.HTML_PREVIEW
                && outputType != OperationReportOutputType.HTML_PRINT) {
            throw new IllegalArgumentException(
                    "プレビュー帳票の形式は画面プレビューまたはブラウザ印刷を指定してください。"
            );
        }
    }

    private boolean isHtmlPreview(OperationReportPreview definition) {
        return definition.getOutputType() == OperationReportOutputType.HTML_PREVIEW
                || definition.getOutputType() == OperationReportOutputType.HTML_PRINT;
    }

    private PreviewReportSettingResponse toResponse(
            OperationReportPreview definition
    ) {
        String key = definition.getHtmlTemplateKey();
        return new PreviewReportSettingResponse(
                definition.getId(),
                definition.getOperationType(),
                definition.getReportCode(),
                definition.getReportName(),
                definition.getTableName(),
                definition.getFilterColumnName(),
                definition.getTargetParamName(),
                definition.getOrderBy(),
                definition.getDisplayOrder(),
                definition.getOutputType(),
                definition.getActiveFlag(),
                key,
                TEMPLATE_VERSION,
                definition.getHtmlTemplateHash(),
                StringUtils.hasText(key) && storageService.exists(key)
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256を利用できません。", e);
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("tenantIdを取得できません。");
        }
        return tenantId;
    }
}
