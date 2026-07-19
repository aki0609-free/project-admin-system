package com.project.backend.features.system.imports.service.resolver;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;

@Service
public class ImportColumnResolver {

    public List<ImportColumnDefinition> resolveImportColumns(
            ImportTargetDefinition target
    ) {
        List<ImportColumnDefinition> columns = target.columns()
                .stream()
                .sorted((a, b) -> Integer.compare(a.orderNo(), b.orderNo()))
                .toList();

        if (columns.isEmpty()) {
            throw new RuntimeException(
                    "取込対象カラムが定義されていません。 targetCode="
                            + target.targetCode()
            );
        }

        return columns;
    }

    public List<ImportColumnDefinition> resolveKeyColumns(
            ImportTargetDefinition target,
            List<ImportColumnDefinition> columns
    ) {
        List<ImportColumnDefinition> keyColumns = columns.stream()
                .filter(ImportColumnDefinition::keyFlag)
                .toList();

        if (keyColumns.isEmpty()) {
            throw new RuntimeException(
                    "UPDATE_ONLY / UPSERT では keyFlag=true のカラムが必要です。 targetCode="
                            + target.targetCode()
            );
        }

        return keyColumns;
    }

    public List<ImportColumnDefinition> resolveUpdateColumns(
            ImportTargetDefinition target,
            List<ImportColumnDefinition> columns
    ) {
        List<ImportColumnDefinition> updateColumns = columns.stream()
                .filter(column -> !column.keyFlag())
                .filter(ImportColumnDefinition::updatableFlag)
                .toList();

        if (updateColumns.isEmpty()) {
            throw new RuntimeException(
                    "更新対象カラムがありません。updatableFlag=true かつ keyFlag=false のカラムを設定してください。 targetCode="
                            + target.targetCode()
            );
        }

        return updateColumns;
    }
}