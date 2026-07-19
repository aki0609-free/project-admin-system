package com.project.backend.features.system.backup.service.resolver;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.project.backend.features.system.backup.dto.BackupColumnDefinition;
import com.project.backend.features.system.backup.dto.BackupTargetDefinition;

@Service
public class BackupExportColumnResolver {

    public List<BackupColumnDefinition> resolve(
            BackupTargetDefinition target
    ) {
        List<BackupColumnDefinition> columns =
                target.columns()
                        .stream()
                        .filter(BackupColumnDefinition::exportFlag)
                        .sorted(
                                (a, b) ->
                                        Integer.compare(
                                                a.orderNo(),
                                                b.orderNo()
                                        )
                        )
                        .toList();

        if (CollectionUtils.isEmpty(columns)) {
            throw new RuntimeException(
                    "出力対象カラムが定義されていません。 targetCode="
                            + target.targetCode()
            );
        }

        return columns;
    }
}