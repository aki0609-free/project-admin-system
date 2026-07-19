package com.project.backend.features.system.backup.service.validation;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.backup.dto.BackupColumnSaveRequest;
import com.project.backend.features.system.backup.dto.BackupTargetSaveRequest;
import com.project.backend.features.system.backup.enums.BackupOutputMode;

@Service
public class BackupTargetValidator {

    public void validate(
            BackupTargetSaveRequest request
    ) {
        validateRequest(request);

        validateOutputSetting(request);

        validateColumns(request);
    }

    private void validateRequest(
            BackupTargetSaveRequest request
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
    }

    private void validateOutputSetting(
            BackupTargetSaveRequest request
    ) {
        BackupOutputMode outputMode =
                request.outputMode();

        if (
                outputMode == BackupOutputMode.SERVER_FILE
                        || outputMode == BackupOutputMode.BOTH
        ) {
            if (!StringUtils.hasText(request.outputDir())) {
                throw new RuntimeException(
                        "SERVER_FILE または BOTH の場合は outputDir が必須です。"
                );
            }
        }
    }

    private void validateColumns(
            BackupTargetSaveRequest request
    ) {
        if (
                request.columns() == null
                        || request.columns().isEmpty()
        ) {
            throw new RuntimeException(
                    "バックアップ対象カラムを1件以上登録してください。"
            );
        }

        Set<Integer> orderNos =
                new HashSet<>();

        for (BackupColumnSaveRequest column : request.columns()) {

            validateColumn(column);

            if (!orderNos.add(column.orderNo())) {
                throw new RuntimeException(
                        "orderNo が重複しています。 orderNo="
                                + column.orderNo()
                );
            }
        }
    }

    private void validateColumn(
            BackupColumnSaveRequest column
    ) {
        if (column == null) {
            throw new RuntimeException(
                    "カラム定義が不正です。"
            );
        }

        if (!StringUtils.hasText(column.columnName())) {
            throw new RuntimeException(
                    "columnName は必須です。"
            );
        }

        if (!StringUtils.hasText(column.csvHeaderName())) {
            throw new RuntimeException(
                    "csvHeaderName は必須です。 column="
                            + column.columnName()
            );
        }

        if (column.dataType() == null) {
            throw new RuntimeException(
                    "dataType は必須です。 column="
                            + column.columnName()
            );
        }

        if (column.orderNo() == null) {
            throw new RuntimeException(
                    "orderNo は必須です。 column="
                            + column.columnName()
            );
        }

        if (column.orderNo() <= 0) {
            throw new RuntimeException(
                    "orderNo は1以上である必要があります。 column="
                            + column.columnName()
            );
        }
    }
}