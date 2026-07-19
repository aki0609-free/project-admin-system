package com.project.backend.features.system.excelbook.service.parser;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

@Component
public class ExcelTemplateParser {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\$\\{([^}]+)}");

    public void writeToBook(
            Path templatePath,
            Path outputPath,
            String templateSheetName,
            String targetSheetName,
            Map<String, Object> context
    ) {
        try {
            Files.createDirectories(outputPath.getParent());

            Workbook workbook = openWorkbook(templatePath, outputPath);

            Sheet templateSheet = workbook.getSheet(templateSheetName);
            if (templateSheet == null) {
                throw new IllegalStateException("テンプレートシートが見つかりません: " + templateSheetName);
            }

            removeSheetIfExists(workbook, targetSheetName);

            Sheet sheet = workbook.cloneSheet(workbook.getSheetIndex(templateSheet));
            workbook.setSheetName(workbook.getSheetIndex(sheet), targetSheetName);

            fillSheet(sheet, context);

            try (OutputStream out = Files.newOutputStream(outputPath)) {
                workbook.write(out);
            }

            workbook.close();

        } catch (Exception e) {
            throw new RuntimeException("Excel台帳更新に失敗しました。", e);
        }
    }

    private Workbook openWorkbook(Path templatePath, Path outputPath) throws Exception {
        if (Files.exists(outputPath)) {
            try (InputStream in = Files.newInputStream(outputPath)) {
                return WorkbookFactory.create(in);
            }
        }

        try (InputStream in = Files.newInputStream(templatePath)) {
            return WorkbookFactory.create(in);
        }
    }

    private void removeSheetIfExists(Workbook workbook, String sheetName) {
        int index = workbook.getSheetIndex(sheetName);
        if (index >= 0) {
            workbook.removeSheetAt(index);
        }
    }

    @SuppressWarnings("unchecked")
    private void fillSheet(Sheet sheet, Map<String, Object> context) {
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) context.getOrDefault("rows", List.of());

        int rowTemplateIndex = findRowsTemplateIndex(sheet);

        replaceNormalPlaceholders(sheet, context);

        if (rowTemplateIndex >= 0) {
            expandRows(sheet, rowTemplateIndex, rows);
        }
    }

    private int findRowsTemplateIndex(Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING
                        && cell.getStringCellValue().contains("${rows.")) {
                    return row.getRowNum();
                }
            }
        }
        return -1;
    }

    private void replaceNormalPlaceholders(
            Sheet sheet,
            Map<String, Object> context
    ) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() != CellType.STRING) continue;

                String value = cell.getStringCellValue();
                if (!value.contains("${") || value.contains("${rows.")) continue;

                cell.setCellValue(replaceText(value, context));
            }
        }
    }

    private void expandRows(
            Sheet sheet,
            int rowTemplateIndex,
            List<Map<String, Object>> rows
    ) {
        Row templateRow = sheet.getRow(rowTemplateIndex);

        if (rows.isEmpty()) {
            removeRow(sheet, rowTemplateIndex);
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            int targetRowIndex = rowTemplateIndex + i;

            if (i > 0) {
                sheet.shiftRows(targetRowIndex, sheet.getLastRowNum(), 1, true, false);
                Row newRow = sheet.createRow(targetRowIndex);
                copyRow(templateRow, newRow);
            }

            Row row = sheet.getRow(targetRowIndex);
            replaceRowPlaceholders(row, rows.get(i));
        }
    }

    private void replaceRowPlaceholders(
            Row row,
            Map<String, Object> rowData
    ) {
        if (row == null) return;

        for (Cell cell : row) {
            if (cell.getCellType() != CellType.STRING) continue;

            String value = cell.getStringCellValue();
            if (!value.contains("${rows.")) continue;

            Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);

            if (matcher.matches()) {
                String key = matcher.group(1).replaceFirst("^rows\\.", "");
                setCellValue(cell, rowData.get(key));
            } else {
                cell.setCellValue(replaceRowsText(value, rowData));
            }
        }
    }

    private String replaceText(String text, Map<String, Object> context) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            Object value = context.get(matcher.group(1));
            matcher.appendReplacement(
                    sb,
                    Matcher.quoteReplacement(value == null ? "" : String.valueOf(value))
            );
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceRowsText(String text, Map<String, Object> rowData) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).replaceFirst("^rows\\.", "");
            Object value = rowData.get(key);
            matcher.appendReplacement(
                    sb,
                    Matcher.quoteReplacement(value == null ? "" : String.valueOf(value))
            );
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }

        if (value instanceof Date date) {
            cell.setCellValue(date.toLocalDate());
            return;
        }

        if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            return;
        }

        cell.setCellValue(String.valueOf(value));
    }

    private void copyRow(Row sourceRow, Row targetRow) {
        targetRow.setHeight(sourceRow.getHeight());

        for (int i = sourceRow.getFirstCellNum(); i < sourceRow.getLastCellNum(); i++) {
            Cell sourceCell = sourceRow.getCell(i);
            if (sourceCell == null) continue;

            Cell targetCell = targetRow.createCell(i);
            targetCell.setCellStyle(sourceCell.getCellStyle());

            switch (sourceCell.getCellType()) {
                case STRING -> targetCell.setCellValue(sourceCell.getStringCellValue());
                case NUMERIC -> targetCell.setCellValue(sourceCell.getNumericCellValue());
                case BOOLEAN -> targetCell.setCellValue(sourceCell.getBooleanCellValue());
                case FORMULA -> targetCell.setCellFormula(sourceCell.getCellFormula());
                case BLANK -> targetCell.setBlank();
                default -> targetCell.setCellValue(sourceCell.toString());
            }
        }
    }

    private void removeRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) return;

        sheet.removeRow(row);

        if (rowIndex < sheet.getLastRowNum()) {
            sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
        }
    }
}