package com.project.backend.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvLineToMapProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void 指定した文字コード区切り文字とヘッダー行を使う()
            throws Exception {
        Path csv = tempDir.resolve("sample.csv");
        Files.writeString(
                csv,
                "説明行\nemployeeCode;amount\nE001;1200\n"
        );

        CsvLineToMapProcessor processor =
                new CsvLineToMapProcessor(
                        csv.toString(),
                        "UTF-8",
                        ';',
                        2
                );

        Map<String, String> row =
                processor.process("E001;1200");

        assertThat(row)
                .containsEntry("employeeCode", "E001")
                .containsEntry("amount", "1200");
    }
}
