package com.project.backend.features.system.imports.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportWriteError;
import com.project.backend.features.system.imports.dto.ImportWriteResult;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.service.builder.ImportRowParamBuilder;
import com.project.backend.features.system.imports.service.builder.ImportWriteErrorBuilder;
import com.project.backend.features.system.imports.service.counter.ImportWriteCounter;
import com.project.backend.features.system.imports.service.executor.ImportWriteExecutor;
import com.project.backend.features.system.imports.service.resolver.ImportColumnResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvImportWriteService {

        private final ImportTargetAdminService importTargetAdminService;
        private final ImportColumnResolver columnResolver;
        private final ImportRowParamBuilder rowParamBuilder;
        private final ImportWriteExecutor writeExecutor;
        private final ImportWriteErrorBuilder errorBuilder;

        public void prepareBeforeWrite(String targetCode) {
                ImportTargetDefinition target = importTargetAdminService.findByTargetCode(targetCode);

                if (target.importMode() == ImportMode.DELETE_INSERT) {
                        writeExecutor.executeDeleteAll(target);
                }
        }

        public ImportWriteResult writeRows(
                        String targetCode,
                        List<? extends Map<String, String>> rows) {
                if (CollectionUtils.isEmpty(rows)) {
                        return emptyResult();
                }

                ImportTargetDefinition target = importTargetAdminService.findByTargetCode(targetCode);

                List<ImportColumnDefinition> columns = columnResolver.resolveImportColumns(target);

                ImportWriteCounter counter = new ImportWriteCounter();

                List<ImportWriteError> errors = new ArrayList<>();

                int rowNo = resolveFirstDataRowNo(target);

                for (Map<String, String> row : rows) {
                        counter.countTotal();

                        try {
                                Map<String, Object> params = rowParamBuilder.build(
                                                row,
                                                columns);

                                executeOne(
                                                target,
                                                columns,
                                                params,
                                                counter);

                        } catch (Exception e) {
                                counter.countError();

                                errors.add(
                                                errorBuilder.build(
                                                                rowNo,
                                                                row,
                                                                e));
                        }

                        rowNo++;
                }

                return counter.toResult(errors);
        }

        private void executeOne(
                        ImportTargetDefinition target,
                        List<ImportColumnDefinition> columns,
                        Map<String, Object> params,
                        ImportWriteCounter counter) {
                switch (target.importMode()) {
                        case INSERT_ONLY, DELETE_INSERT -> {
                                writeExecutor.executeInsert(
                                                target,
                                                columns,
                                                params);

                                counter.countInserted();
                        }

                        case UPDATE_ONLY -> executeUpdate(
                                        target,
                                        columns,
                                        params,
                                        counter);

                        case UPSERT -> executeUpsert(
                                        target,
                                        columns,
                                        params,
                                        counter);

                        default -> throw new RuntimeException(
                                        "未対応のimportModeです。 importMode="
                                                        + target.importMode()
                                                        + ", targetCode="
                                                        + target.targetCode());
                }
        }

        private void executeUpdate(
                        ImportTargetDefinition target,
                        List<ImportColumnDefinition> columns,
                        Map<String, Object> params,
                        ImportWriteCounter counter) {
                int updated = writeExecutor.executeUpdate(
                                target,
                                columns,
                                params);

                if (updated > 0) {
                        counter.countUpdated(updated);
                        return;
                }

                counter.countSkipped();
        }

        private void executeUpsert(
                        ImportTargetDefinition target,
                        List<ImportColumnDefinition> columns,
                        Map<String, Object> params,
                        ImportWriteCounter counter) {
                boolean exists = writeExecutor.exists(
                                target,
                                columns,
                                params);

                if (exists) {
                        executeUpdate(
                                        target,
                                        columns,
                                        params,
                                        counter);
                        return;
                }

                writeExecutor.executeInsert(
                                target,
                                columns,
                                params);

                counter.countInserted();
        }

        private ImportWriteResult emptyResult() {
                return ImportWriteResult.builder()
                                .totalCount(0)
                                .insertedCount(0)
                                .updatedCount(0)
                                .skippedCount(0)
                                .errorCount(0)
                                .errors(List.of())
                                .build();
        }

        private int resolveFirstDataRowNo(
                        ImportTargetDefinition target) {
                return target.dataStartRowNumber() != null
                                ? target.dataStartRowNumber()
                                : 2;
        }
}