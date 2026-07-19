package com.project.backend.features.system.imports.service.validation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.imports.dto.ImportColumnSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTargetValidator {

    private final ImportTargetRepository repository;

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
        validateColumns(request);
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
    }

    private void validateSourceSetting(
            ImportTargetSaveRequest request
    ) {
        if (
                request.sourceType() == ImportSourceType.SERVER_FILE
                        && !StringUtils.hasText(request.fixedFilePath())
        ) {
            throw new RuntimeException(
                    "sourceType=SERVER_FILE の場合、fixedFilePath は必須です。"
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
    }

    private void validateColumns(
            ImportTargetSaveRequest request
    ) {
        List<ImportColumnSaveRequest> columns =
                request.columns() != null
                        ? request.columns()
                        : List.of();

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

            if (column.orderNo() <= 0) {
                throw new RuntimeException(
                        "orderNo は1以上で指定してください。"
                );
            }
        }
    }
}