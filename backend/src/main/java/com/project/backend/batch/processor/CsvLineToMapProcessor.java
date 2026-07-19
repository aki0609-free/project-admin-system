package com.project.backend.batch.processor;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;

public class CsvLineToMapProcessor implements ItemProcessor<String, Map<String, String>> {

    private final String[] headers;
    private final CSVParser parser;

    public CsvLineToMapProcessor(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            String headerLine = reader.readLine();

            this.parser = new CSVParserBuilder()
                    .withSeparator(',')
                    .build();

            this.headers = parser.parseLine(headerLine);
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