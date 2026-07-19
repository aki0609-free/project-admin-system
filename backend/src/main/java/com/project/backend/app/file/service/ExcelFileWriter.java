package com.project.backend.app.file.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelFileWriter {

    public byte[] write(List<Map<String, Object>> rows, List<String> headers, String sheetName) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            var sheet = workbook.createSheet(sheetName != null ? sheetName : "Sheet1");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Map<String, Object> rowMap = rows.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);

                for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                    Object value = rowMap.get(headers.get(colIndex));
                    row.createCell(colIndex).setCellValue(value != null ? String.valueOf(value) : "");
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel出力に失敗しました。", e);
        }
    }
}