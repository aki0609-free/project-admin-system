package com.project.backend.app.file.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.opencsv.CSVWriter;

@Component
public class CsvFileWriter {

    public byte[] write(List<Map<String, Object>> rows, List<String> headers) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (
                    OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                    CSVWriter csvWriter = new CSVWriter(writer)) {
                csvWriter.writeNext(headers.toArray(new String[0]));

                for (Map<String, Object> row : rows) {
                    String[] line = headers.stream()
                            .map(header -> formatValue(row.get(header)))
                            .toArray(String[]::new);
                    csvWriter.writeNext(line);
                }

                writer.flush();
            }

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("CSV出力に失敗しました。", e);
        }
    }

    public byte[] write(
            List<Map<String, Object>> rows,
            List<String> columnKeys,
            List<String> headerNames,
            boolean includeHeader) {
        validate(columnKeys, headerNames);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (
                    OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                    CSVWriter csvWriter = new CSVWriter(writer)) {
                if (includeHeader) {
                    csvWriter.writeNext(headerNames.toArray(new String[0]));
                }

                for (Map<String, Object> row : rows) {
                    String[] line = columnKeys.stream()
                            .map(columnKey -> formatValue(row.get(columnKey)))
                            .toArray(String[]::new);

                    csvWriter.writeNext(line);
                }

                writer.flush();
            }

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("CSV出力に失敗しました。", e);
        }
    }

    private void validate(
            List<String> columnKeys,
            List<String> headerNames) {
        if (CollectionUtils.isEmpty(columnKeys)) {
            throw new RuntimeException("CSV出力項目がありません。");
        }

        if (CollectionUtils.isEmpty(headerNames)) {
            throw new RuntimeException("CSVヘッダー定義がありません。");
        }

        if (columnKeys.size() != headerNames.size()) {
            throw new RuntimeException("CSV出力項目とヘッダー数が一致しません。");
        }
    }

    private String formatValue(Object value) {
        if (value == null)
            return "";

        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        if (value instanceof LocalDate date) {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return String.valueOf(value);
    }
}