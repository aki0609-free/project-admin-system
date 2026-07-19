package com.project.backend.batch.reader;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.support.AbstractItemStreamItemReader;

import com.opencsv.CSVReader;
import com.project.backend.batch.dto.CsvImportRow;

public class GenericCsvItemReader extends AbstractItemStreamItemReader<CsvImportRow> {

    private final Path filePath;
    private final List<String> headers;

    private CSVReader csvReader;
    private long rowNo = 0;
    private boolean initialized = false;

    public GenericCsvItemReader(Path filePath, List<String> headers) {
        this.filePath = filePath;
        this.headers = headers;
        setName("genericCsvItemReader");
    }

    @Override
    public CsvImportRow read() throws Exception {
        if (!initialized) {
            this.csvReader = new CSVReader(new FileReader(filePath.toFile()));
            this.csvReader.readNext(); // ヘッダー1行スキップ
            initialized = true;
        }

        String[] line = csvReader.readNext();
        if (line == null) {
            return null;
        }

        rowNo++;

        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = headers.get(i);
            String value = i < line.length ? line[i] : null;
            values.put(key, value);
        }

        return new CsvImportRow(rowNo, values);
    }

    @Override
    public void close() {
        try {
            if (csvReader != null) {
                csvReader.close();
            }
        } catch (Exception ignored) {
        }
    }
}