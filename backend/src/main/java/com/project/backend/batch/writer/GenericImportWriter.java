package com.project.backend.batch.writer;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.project.backend.batch.dto.ProcessedImportRow;

public class GenericImportWriter implements ItemWriter<ProcessedImportRow> {

    private final String tableName;
    private final List<String> columnNames;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GenericImportWriter(
            String tableName,
            List<String> columnNames,
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("null")
    @Override
    public void write(Chunk<? extends ProcessedImportRow> chunk) {
        String sql = buildInsertSql();

        for (ProcessedImportRow row : chunk.getItems()) {
            jdbcTemplate.update(sql, row.values());
        }
    }

    private String buildInsertSql() {
        String columns = String.join(", ", columnNames);
        String values = columnNames.stream()
                .map(name -> ":" + name)
                .collect(Collectors.joining(", "));

        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
    }
}