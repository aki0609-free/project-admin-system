package com.project.backend.batch.processor;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;

public class CsvLineToMapProcessor implements ItemProcessor<String, Map<String, String>> {

    private final String[] headers;
    private final CSVParser parser;

    public CsvLineToMapProcessor(
            String filePath,
            String charset,
            char delimiter,
            int headerRowNumber
    ) {
        try (BufferedReader reader = Files.newBufferedReader(
                Path.of(filePath),
                Charset.forName(charset)
        )) {
            for (int rowNo = 1; rowNo < headerRowNumber; rowNo++) {
                if (reader.readLine() == null) {
                    throw new IllegalArgumentException(
                            "CSVのヘッダー行が存在しません。 headerRowNumber="
                                    + headerRowNumber
                    );
                }
            }

            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw new IllegalArgumentException(
                        "CSVのヘッダー行が存在しません。 headerRowNumber="
                                + headerRowNumber
                );
            }

            this.parser = new CSVParserBuilder()
                    .withSeparator(delimiter)
                    .build();

            this.headers = parser.parseLine(headerLine);
            if (headers.length > 0 && headers[0].startsWith("\uFEFF")) {
                headers[0] = headers[0].substring(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("CSVヘッダーの読み込みに失敗しました。", e);
        }
    }

    @SuppressWarnings("null")
    @Override
    public Map<String, String> process(String line) throws Exception {
        String[] values = parser.parseLine(line);

        Map<String, String> row = new LinkedHashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            String value = i < values.length ? values[i] : "";
            row.put(header, value);
        }

        return row;
    }
}
