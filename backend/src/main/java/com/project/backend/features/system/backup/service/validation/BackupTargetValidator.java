package com.project.backend.features.system.backup.service.validation;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.backup.dto.BackupColumnSaveRequest;
import com.project.backend.features.system.backup.dto.BackupSourceSchema;
import com.project.backend.features.system.backup.dto.BackupTargetSaveRequest;
import com.project.backend.features.system.backup.enums.BackupOutputMode;
import com.project.backend.features.system.backup.repository.BackupTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupTargetValidator {

    private static final Pattern TARGET_CODE_PATTERN =
            Pattern.compile("[A-Z][A-Z0-9_]{1,99}");

    private final BackupTargetRepository repository;
    private final BackupSchemaInspector schemaInspector;

    public void validate(
            BackupTargetSaveRequest request,
            Long id
    ) {
        validateRequest(request);
        validateImmutableFields(request, id);
        validateDuplicate(request, id);
        validateOutputSetting(request);

        BackupSourceSchema schema =
                schemaInspector.inspect(request.tableName());
        validateColumns(request, schema);
    }

    private void validateRequest(BackupTargetSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }
        if (!StringUtils.hasText(request.targetCode())
                || !TARGET_CODE_PATTERN.matcher(request.targetCode()).matches()) {
            throw new RuntimeException(
                    "targetCode は英大文字で始まる英大文字・数字・アンダースコアで指定してください。"
            );
        }
        if (!StringUtils.hasText(request.targetName())) {
            throw new RuntimeException("targetName は必須です。");
        }
        if (request.targetName().length() > 200) {
            throw new RuntimeException("targetName は200文字以内です。");
        }
        if (!StringUtils.hasText(request.tableName())) {
            throw new RuntimeException("tableName は必須です。");
        }
        schemaInspector.validateIdentifier(request.tableName(), "tableName");
        if (request.description() != null && request.description().length() > 500) {
            throw new RuntimeException("description は500文字以内です。");
        }
        if (request.fileNamePattern() != null
                && request.fileNamePattern().length() > 200) {
            throw new RuntimeException("fileNamePattern は200文字以内です。");
        }
        validateFileNamePattern(request.fileNamePattern());
    }

    private void validateImmutableFields(
            BackupTargetSaveRequest request,
            Long id
    ) {
        if (id == null) {
            return;
        }

        repository.findByIdAndTenantIdAndDeletedAtIsNull(
                id,
                requireTenantId()
        ).ifPresent(existing -> {
            if (!existing.getTargetCode().equals(request.targetCode())) {
                throw new RuntimeException("作成後のtargetCodeは変更できません。");
            }
            if (!existing.getTableName().equals(request.tableName())) {
                throw new RuntimeException("作成後のtableNameは変更できません。");
            }
        });
    }

    private void validateDuplicate(
            BackupTargetSaveRequest request,
            Long id
    ) {
        boolean exists = id == null
                ? repository.existsByTenantIdAndTargetCodeAndDeletedAtIsNull(
                        requireTenantId(),
                        request.targetCode()
                )
                : repository.existsByTenantIdAndTargetCodeAndIdNotAndDeletedAtIsNull(
                        requireTenantId(),
                        request.targetCode(),
                        id
                );

        if (exists) {
            throw new RuntimeException(
                    "targetCode が重複しています。 targetCode=" + request.targetCode()
            );
        }
    }

    private void validateOutputSetting(BackupTargetSaveRequest request) {
        BackupOutputMode outputMode = request.outputMode() != null
                ? request.outputMode()
                : BackupOutputMode.DOWNLOAD;

        if ((outputMode == BackupOutputMode.SERVER_FILE
                || outputMode == BackupOutputMode.BOTH)
                && !StringUtils.hasText(request.outputDir())) {
            throw new RuntimeException(
                    "ストレージ保存を行う場合は outputDir が必須です。"
            );
        }
        if (StringUtils.hasText(request.outputDir())) {
            validateRelativeDirectory(request.outputDir());
        }
    }

    private void validateColumns(
            BackupTargetSaveRequest request,
            BackupSourceSchema schema
    ) {
        if (request.columns() == null || request.columns().isEmpty()) {
            throw new RuntimeException(
                    "バックアップ対象カラムを1件以上登録してください。"
            );
        }

        Set<String> columnNames = new HashSet<>();
        Set<String> headerNames = new HashSet<>();
        Set<Integer> orderNos = new HashSet<>();
        boolean hasExportColumn = false;

        for (BackupColumnSaveRequest column : request.columns()) {
            validateColumn(column, schema);

            String normalizedColumn = column.columnName().toLowerCase();
            if (!columnNames.add(normalizedColumn)) {
                throw new RuntimeException(
                        "columnName が重複しています。 columnName=" + column.columnName()
                );
            }
            if (!headerNames.add(column.csvHeaderName())) {
                throw new RuntimeException(
                        "csvHeaderName が重複しています。 csvHeaderName="
                                + column.csvHeaderName()
                );
            }
            if (!orderNos.add(column.orderNo())) {
                throw new RuntimeException(
                        "orderNo が重複しています。 orderNo=" + column.orderNo()
                );
            }
            hasExportColumn |= !Boolean.FALSE.equals(column.exportFlag());
        }

        if (!hasExportColumn) {
            throw new RuntimeException("出力対象カラムを1件以上選択してください。");
        }
    }

    private void validateColumn(
            BackupColumnSaveRequest column,
            BackupSourceSchema schema
    ) {
        if (column == null) {
            throw new RuntimeException("カラム定義が不正です。");
        }
        schemaInspector.validateIdentifier(column.columnName(), "columnName");
        if (!schema.containsColumn(column.columnName())) {
            throw new RuntimeException(
                    "対象テーブルに存在しないカラムです。 columnName="
                            + column.columnName()
            );
        }
        if (!StringUtils.hasText(column.csvHeaderName())) {
            throw new RuntimeException(
                    "csvHeaderName は必須です。 column=" + column.columnName()
            );
        }
        if (column.csvHeaderName().length() > 200) {
            throw new RuntimeException("csvHeaderName は200文字以内です。");
        }
        if (column.dataType() == null) {
            throw new RuntimeException(
                    "dataType は必須です。 column=" + column.columnName()
            );
        }
        if (column.orderNo() == null || column.orderNo() <= 0) {
            throw new RuntimeException(
                    "orderNo は1以上である必要があります。 column="
                            + column.columnName()
            );
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new RuntimeException("テナント情報を取得できません。");
        }
        return tenantId;
    }

    private void validateFileNamePattern(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return;
        }
        if (pattern.contains("/") || pattern.contains("\\")) {
            throw new RuntimeException("fileNamePattern にパスを含めることはできません。");
        }
        String remaining = pattern
                .replace("{targetCode}", "")
                .replace("{timestamp}", "");
        if (remaining.contains("{") || remaining.contains("}")) {
            throw new RuntimeException(
                    "fileNamePatternで利用できる変数は{targetCode}と{timestamp}だけです。"
            );
        }
        if (!pattern.toLowerCase().endsWith(".csv")) {
            throw new RuntimeException("fileNamePattern は.csvで終了してください。");
        }
        if (!pattern.contains("{timestamp}")) {
            throw new RuntimeException(
                    "ファイルの上書きを防ぐためfileNamePatternには{timestamp}が必要です。"
            );
        }
    }

    private void validateRelativeDirectory(String outputDir) {
        if (outputDir.length() > 500) {
            throw new RuntimeException("outputDir は500文字以内です。");
        }
        String normalized = outputDir.replace("\\", "/");
        if (normalized.startsWith("/")) {
            throw new RuntimeException("outputDir は相対パスで指定してください。");
        }
        for (String segment : normalized.split("/", -1)) {
            if (!StringUtils.hasText(segment)
                    || ".".equals(segment)
                    || "..".equals(segment)
                    || segment.chars().anyMatch(Character::isISOControl)) {
                throw new RuntimeException("outputDir に不正なパスが含まれています。");
            }
        }
    }
}
