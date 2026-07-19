package com.project.backend.features.system.backup.service.builder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.opencsv.CSVWriter;
import com.project.backend.features.system.backup.dto.BackupColumnDefinition;
import com.project.backend.features.system.backup.dto.BackupDataResult;

@Component
public class BackupCsvBuilder {

    public byte[] build(
            List<BackupColumnDefinition> columns,
            BackupDataResult data,
            boolean includeHeader
    ) {
        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                CSVWriter writer = new CSVWriter(
                        new OutputStreamWriter(
                                out,
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            if (includeHeader) {
                writer.writeNext(
                        columns.stream()
                                .map(BackupColumnDefinition::csvHeaderName)
                                .toArray(String[]::new)
                );
            }

            for (Map<String, Object> row : data.rows()) {

                writer.writeNext(
                        columns.stream()
                                .map(c -> {
                                    Object value =
                                            row.get(c.columnName());

                                    return value == null
                                            ? ""
                                            : String.valueOf(value);
                                })
                                .toArray(String[]::new)
                );
            }

            writer.flush();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "CSV生成に失敗しました。",
                    e
            );
        }
    }
}