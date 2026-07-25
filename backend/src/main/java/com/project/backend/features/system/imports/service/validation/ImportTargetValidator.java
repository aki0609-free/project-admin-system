package com.project.backend.features.system.imports.service.validation;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.imports.dto.ImportColumnSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.entity.ImportTargetCatalog;
import com.project.backend.features.system.imports.entity.ImportTargetCatalogColumn;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;
import com.project.backend.features.system.imports.service.ImportTargetCatalogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTargetValidator {

    private static final Pattern TARGET_CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]{2,99}$");

    private final ImportTargetRepository repository;
    private final ImportTargetCatalogService catalogService;

    public void validateForCreate(
            ImportTargetSaveRequest request
    ) {
        validate(
                request,
                null
        );
    }

    public void validateForUpdate(
            Long id,
            ImportTargetSaveRequest request
    ) {
        validate(
                request,
                id
        );
    }

    private void validate(
            ImportTargetSaveRequest request,
            Long id
    ) {
        validateRequired(request);
        validateSourceSetting(request);
        validateDuplicate(request, id);
        validateCatalogAndColumns(request);
    }

    private void validateRequired(
            ImportTargetSaveRequest request
    ) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.targetCode())) {
            throw new RuntimeException("targetCode は必須です。");
        }

        if (!TARGET_CODE_PATTERN.matcher(request.targetCode().trim()).matches()) {
            throw new RuntimeException(
                    "targetCode は英大文字・数字・アンダースコアで指定してください。"
            );
        }

        if (!StringUtils.hasText(request.targetName())) {
            throw new RuntimeException("targetName は必須です。");
        }

        if (!StringUtils.hasText(request.tableName())) {
            throw new RuntimeException("tableName は必須です。");
        }

        if (request.sourceType() == null) {
            throw new RuntimeException("sourceType は必須です。");
        }

        if (request.importMode() == null) {
            throw new RuntimeException("importMode は必須です。");
        }

        if (request.headerRowNumber() == null
                || request.headerRowNumber() < 1) {
            throw new RuntimeException(
                    "headerRowNumber は1以上で指定してください。"
            );
        }

        if (request.dataStartRowNumber() == null
                || request.dataStartRowNumber()
                        <= request.headerRowNumber()) {
            throw new RuntimeException(
                    "dataStartRowNumber はheaderRowNumberより後にしてください。"
            );
        }

        if (!StringUtils.hasText(request.charset())) {
            throw new RuntimeException("charset は必須です。");
        }

        if (!Charset.isSupported(request.charset().trim())) {
            throw new RuntimeException(
                    "未対応のcharsetです。 charset="
                            + request.charset()
            );
        }

        if (request.delimiter() == null
                || request.delimiter().length() != 1) {
            throw new RuntimeException(
                    "delimiter は1文字で指定してください。"
            );
        }
    }

    private void validateSourceSetting(
            ImportTargetSaveRequest request
    ) {
        if (request.sourceType() == ImportSourceType.UPLOAD
                && request.scriptType() != null
                && request.scriptType() != ImportScriptType.NONE) {
            throw new RuntimeException(
                    "sourceType=UPLOAD ではscriptTypeを指定できません。"
            );
        }

        if (
                request.sourceType() == ImportSourceType.SERVER_FILE
                        && !StringUtils.hasText(request.fixedFilePath())
        ) {
            throw new RuntimeException(
                    "sourceType=SERVER_FILE の場合、fixedFilePath は必須です。"
            );
        }

        if (request.sourceType() != ImportSourceType.UPLOAD
                && StringUtils.hasText(request.fixedFilePath())
                && !request.fixedFilePath()
                        .trim()
                        .toLowerCase()
                        .endsWith(".csv")) {
            throw new RuntimeException(
                    "fixedFilePathにはCSVファイルを指定してください。"
            );
        }

        if (request.sourceType() == ImportSourceType.SCRIPT) {

            if (
                    request.scriptType() == null
                            || request.scriptType() == ImportScriptType.NONE
            ) {
                throw new RuntimeException(
                        "sourceType=SCRIPT の場合、scriptType は必須です。"
                );
            }

            if (!StringUtils.hasText(request.scriptPath())) {
                throw new RuntimeException(
                        "sourceType=SCRIPT の場合、scriptPath は必須です。"
                );
            }

            if (!StringUtils.hasText(request.fixedFilePath())) {
                throw new RuntimeException(
                        "sourceType=SCRIPT の場合、生成CSVの fixedFilePath は必須です。"
                );
            }

            String expectedExtension =
                    request.scriptType() == ImportScriptType.PYTHON
                            ? ".py"
                            : ".sh";

            if (!request.scriptPath()
                    .trim()
                    .toLowerCase()
                    .endsWith(expectedExtension)) {
                throw new RuntimeException(
                        "scriptTypeとscriptPathの拡張子が一致しません。"
                );
            }
        }
    }

    private void validateDuplicate(
            ImportTargetSaveRequest request,
            Long id
    ) {
        boolean exists =
                id == null
                        ? repository.existsByTargetCodeAndDeletedAtIsNull(
                                request.targetCode()
                        )
                        : repository.existsByTargetCodeAndIdNotAndDeletedAtIsNull(
                                request.targetCode(),
                                id
                        );

        if (exists) {
            throw new RuntimeException(
                    "targetCode が重複しています。 targetCode="
                            + request.targetCode()
            );
        }

        if (id != null) {
            repository.findByIdAndDeletedAtIsNull(id)
                    .filter(entity ->
                            !entity.getTargetCode().equals(
                                    request.targetCode()
                            ))
                    .ifPresent(entity -> {
                        throw new RuntimeException(
                                "作成済みtargetCodeは変更できません。"
                        );
                    });
        }
    }

    private void validateCatalogAndColumns(
            ImportTargetSaveRequest request
    ) {
        ImportTargetCatalog catalog =
                catalogService.findRequired(request.tableName().trim());

        if (request.importMode() == ImportMode.DELETE_INSERT
                && !catalog.isAllowDeleteInsertFlag()) {
            throw new RuntimeException(
                    "この取込先ではDELETE_INSERTは許可されていません。 tableName="
                            + request.tableName()
            );
        }

        List<ImportColumnSaveRequest> columns =
                request.columns() != null
                        ? request.columns()
                        : List.of();

        if (columns.isEmpty()) {
            throw new RuntimeException(
                    "取込対象カラムを1件以上設定してください。"
            );
        }

        Map<String, ImportTargetCatalogColumn> allowedColumns =
                catalog.getColumns().stream()
                        .filter(column ->
                                column.getDeletedAt() == null
                                        && column.isActiveFlag())
                        .collect(Collectors.toMap(
                                ImportTargetCatalogColumn::getColumnName,
                                Function.identity()
                        ));

        Set<String> columnNames = new HashSet<>();
        Set<String> csvHeaderNames = new HashSet<>();
        Set<Integer> orderNumbers = new HashSet<>();

        for (ImportColumnSaveRequest column : columns) {

            if (!StringUtils.hasText(column.columnName())) {
                throw new RuntimeException("columnName は必須です。");
            }

            if (!StringUtils.hasText(column.csvHeaderName())) {
                throw new RuntimeException("csvHeaderName は必須です。");
            }

            if (column.dataType() == null) {
                throw new RuntimeException("dataType は必須です。");
            }

            if (column.orderNo() == null
                    || column.orderNo() <= 0) {
                throw new RuntimeException(
                        "orderNo は1以上で指定してください。"
                );
            }

            String columnName = column.columnName().trim();
            String csvHeaderName = column.csvHeaderName().trim();

            if (!columnNames.add(columnName)) {
                throw new RuntimeException(
                        "columnName が重複しています。 columnName="
                                + columnName
                );
            }

            if (!csvHeaderNames.add(csvHeaderName)) {
                throw new RuntimeException(
                        "csvHeaderName が重複しています。 csvHeaderName="
                                + csvHeaderName
                );
            }

            if (!orderNumbers.add(column.orderNo())) {
                throw new RuntimeException(
                        "orderNo が重複しています。 orderNo="
                                + column.orderNo()
                );
            }

            ImportTargetCatalogColumn allowed =
                    allowedColumns.get(columnName);

            if (allowed == null) {
                throw new RuntimeException(
                        "許可されていない取込先カラムです。 tableName="
                                + request.tableName()
                                + ", columnName="
                                + columnName
                );
            }

            if (allowed.getDataType() != column.dataType()) {
                throw new RuntimeException(
                        "取込カラムのデータ型がカタログと一致しません。 columnName="
                                + columnName
                );
            }
        }

        if ((request.importMode() == ImportMode.UPDATE_ONLY
                || request.importMode() == ImportMode.UPSERT)
                && columns.stream().noneMatch(
                        ImportColumnSaveRequest::keyFlagOrDefault
                )) {
            throw new RuntimeException(
                    "UPDATE_ONLY / UPSERT ではキー項目が必要です。"
            );
        }
    }
}
