package com.project.backend.features.operation.book.service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

/** Syncfusion Workbook JSONをExcelファイルへ変換する。 */
@Service
public class SpreadsheetLedgerExcelExportService {

    private static final int EXCEL_MAX_COLUMN_WIDTH = 255 * 256;

    public byte[] export(JsonNode source) {
        JsonNode workbookNode = source.path("Workbook").isObject()
                ? source.path("Workbook")
                : source;
        JsonNode sheets = workbookNode.path("sheets");
        if (!sheets.isArray() || sheets.isEmpty()) {
            throw new IllegalArgumentException(
                    "Excel出力対象のシートがありません。"
            );
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Map<String, XSSFCellStyle> styleCache = new HashMap<>();
            for (int index = 0; index < sheets.size(); index++) {
                writeSheet(
                        workbook,
                        sheets.get(index),
                        index,
                        styleCache,
                        source.path("projectAdminMetadata")
                );
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "台帳のExcel変換に失敗しました。",
                    exception
            );
        }
    }

    private void writeSheet(
            XSSFWorkbook workbook,
            JsonNode source,
            int sheetIndex,
            Map<String, XSSFCellStyle> styleCache,
            JsonNode metadata
    ) {
        String requestedName = source.path("name").asText(
                "Sheet" + (sheetIndex + 1)
        );
        String sheetName = uniqueSheetName(workbook, requestedName);
        XSSFSheet sheet = workbook.createSheet(sheetName);
        sheet.setDisplayGridlines(source.path("showGridLines").asBoolean(true));

        JsonNode columns = source.path("columns");
        if (columns.isArray()) {
            for (int index = 0; index < columns.size(); index++) {
                double width = columns.get(index).path("width").asDouble(64d);
                int excelWidth = (int) Math.round(width / 7d * 256d);
                sheet.setColumnWidth(
                        index,
                        Math.max(256, Math.min(EXCEL_MAX_COLUMN_WIDTH, excelWidth))
                );
            }
        }

        JsonNode rows = source.path("rows");
        if (rows.isArray()) {
            for (int rowOffset = 0; rowOffset < rows.size(); rowOffset++) {
                writeRow(
                        workbook,
                        sheet,
                        rows.get(rowOffset),
                        rowOffset,
                        styleCache
                );
            }
        }

        int frozenRows = source.path("frozenRows").asInt(0);
        int frozenColumns = source.path("frozenColumns").asInt(0);
        if (frozenRows > 0 || frozenColumns > 0) {
            sheet.createFreezePane(frozenColumns, frozenRows);
        }
        applyPrintSettings(sheet, metadata);
    }

    private void writeRow(
            XSSFWorkbook workbook,
            XSSFSheet sheet,
            JsonNode source,
            int rowOffset,
            Map<String, XSSFCellStyle> styleCache
    ) {
        int rowIndex = source.path("index").asInt(rowOffset);
        XSSFRow row = sheet.createRow(rowIndex);
        if (source.has("height")) {
            row.setHeightInPoints((float) source.path("height").asDouble());
        }

        JsonNode cells = source.path("cells");
        if (!cells.isArray()) {
            return;
        }
        for (int cellOffset = 0; cellOffset < cells.size(); cellOffset++) {
            JsonNode sourceCell = cells.get(cellOffset);
            int columnIndex = sourceCell.path("index").asInt(cellOffset);
            var cell = row.createCell(columnIndex);
            writeCellValue(cell, sourceCell);
            cell.setCellStyle(style(
                    workbook,
                    sourceCell.path("style"),
                    sourceCell.path("format").asText(""),
                    styleCache
            ));
            addMergedRegion(
                    sheet,
                    rowIndex,
                    columnIndex,
                    sourceCell,
                    sourceCell.path("style")
            );
        }
    }

    private void writeCellValue(
            org.apache.poi.xssf.usermodel.XSSFCell cell,
            JsonNode source
    ) {
        String formula = source.path("formula").asText("");
        if (!formula.isBlank()) {
            cell.setCellFormula(formula.startsWith("=")
                    ? formula.substring(1)
                    : formula);
            return;
        }
        JsonNode value = source.get("value");
        if (value == null || value.isNull()) {
            cell.setBlank();
        } else if (value.isNumber()) {
            cell.setCellValue(value.asDouble());
        } else if (value.isBoolean()) {
            cell.setCellValue(value.asBoolean());
        } else {
            cell.setCellValue(value.asText());
        }
    }

    private XSSFCellStyle style(
            XSSFWorkbook workbook,
            JsonNode style,
            String format,
            Map<String, XSSFCellStyle> cache
    ) {
        String key = style.toString() + "|" + format;
        XSSFCellStyle cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        XSSFCellStyle result = workbook.createCellStyle();
        if (!format.isBlank()) {
            result.setDataFormat(workbook.createDataFormat().getFormat(format));
        }
        result.setAlignment(horizontalAlignment(
                style.path("textAlign").asText("")
        ));
        result.setVerticalAlignment(verticalAlignment(
                style.path("verticalAlign").asText("")
        ));
        result.setWrapText(style.path("whiteSpace").asText("")
                .equalsIgnoreCase("normal"));
        applyFill(result, style.path("backgroundColor").asText(""));
        applyBorder(result, style);
        result.setFont(font(workbook, style));
        cache.put(key, result);
        return result;
    }

    private XSSFFont font(XSSFWorkbook workbook, JsonNode style) {
        XSSFFont font = workbook.createFont();
        String family = style.path("fontFamily").asText("");
        if (!family.isBlank()) {
            font.setFontName(family);
        }
        String size = style.path("fontSize").asText("")
                .replace("pt", "")
                .trim();
        if (!size.isBlank()) {
            try {
                font.setFontHeightInPoints((short) Math.round(
                        Double.parseDouble(size)
                ));
            } catch (NumberFormatException ignored) {
                // Syncfusionの既定サイズを維持する。
            }
        }
        font.setBold("bold".equalsIgnoreCase(
                style.path("fontWeight").asText("")
        ));
        font.setItalic("italic".equalsIgnoreCase(
                style.path("fontStyle").asText("")
        ));
        XSSFColor color = color(style.path("color").asText(""));
        if (color != null) {
            font.setColor(color);
        }
        return font;
    }

    private void applyFill(XSSFCellStyle style, String value) {
        XSSFColor color = color(value);
        if (color != null) {
            style.setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }

    private void applyBorder(XSSFCellStyle style, JsonNode source) {
        String border = source.path("border").asText("");
        applyBorderEdge(style, "top", source.path("borderTop").asText(border));
        applyBorderEdge(
                style,
                "right",
                source.path("borderRight").asText(border)
        );
        applyBorderEdge(
                style,
                "bottom",
                source.path("borderBottom").asText(border)
        );
        applyBorderEdge(
                style,
                "left",
                source.path("borderLeft").asText(border)
        );
    }

    private void applyBorderEdge(
            XSSFCellStyle style,
            String edge,
            String value
    ) {
        if (value.isBlank() || value.toLowerCase().contains("none")) {
            return;
        }
        XSSFColor color = colorFromBorder(value);
        switch (edge) {
            case "top" -> {
                style.setBorderTop(BorderStyle.THIN);
                if (color != null) style.setTopBorderColor(color);
            }
            case "right" -> {
                style.setBorderRight(BorderStyle.THIN);
                if (color != null) style.setRightBorderColor(color);
            }
            case "bottom" -> {
                style.setBorderBottom(BorderStyle.THIN);
                if (color != null) style.setBottomBorderColor(color);
            }
            case "left" -> {
                style.setBorderLeft(BorderStyle.THIN);
                if (color != null) style.setLeftBorderColor(color);
            }
            default -> {
                // 呼び出し側で固定された4辺のみを扱う。
            }
        }
    }

    private void addMergedRegion(
            XSSFSheet sheet,
            int rowIndex,
            int columnIndex,
            JsonNode cell,
            JsonNode style
    ) {
        int rowSpan = Math.max(1, cell.path("rowSpan").asInt(1));
        int columnSpan = Math.max(1, cell.path("colSpan").asInt(1));
        if (rowSpan == 1 && columnSpan == 1) {
            return;
        }
        CellRangeAddress region = new CellRangeAddress(
                rowIndex,
                rowIndex + rowSpan - 1,
                columnIndex,
                columnIndex + columnSpan - 1
        );
        sheet.addMergedRegion(region);
        String border = style.path("border").asText("");
        if (hasBorder(style.path("borderTop").asText(border))) {
            RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        }
        if (hasBorder(style.path("borderRight").asText(border))) {
            RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
        }
        if (hasBorder(style.path("borderBottom").asText(border))) {
            RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        }
        if (hasBorder(style.path("borderLeft").asText(border))) {
            RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        }
    }

    private void applyPrintSettings(XSSFSheet sheet, JsonNode metadata) {
        var setup = sheet.getPrintSetup();
        String paperSize = metadata.path("paperSize").asText("A4");
        setup.setPaperSize(switch (paperSize) {
            case "A3" -> PrintSetup.A3_PAPERSIZE;
            case "B5" -> PrintSetup.B5_PAPERSIZE;
            default -> PrintSetup.A4_PAPERSIZE;
        });
        setup.setLandscape("LANDSCAPE".equalsIgnoreCase(
                metadata.path("orientation").asText("")
        ));
        if (metadata.path("fitToOnePage").asBoolean(false)) {
            sheet.setFitToPage(true);
            setup.setFitWidth((short) 1);
            setup.setFitHeight((short) 1);
        }
    }

    private String uniqueSheetName(XSSFWorkbook workbook, String requested) {
        String safe = WorkbookUtil.createSafeSheetName(requested);
        String candidate = safe;
        int suffix = 2;
        while (workbook.getSheet(candidate) != null) {
            String tail = " " + suffix++;
            candidate = safe.substring(
                    0,
                    Math.min(safe.length(), 31 - tail.length())
            ) + tail;
        }
        return candidate;
    }

    private HorizontalAlignment horizontalAlignment(String value) {
        return switch (value.toLowerCase()) {
            case "center" -> HorizontalAlignment.CENTER;
            case "right" -> HorizontalAlignment.RIGHT;
            case "justify" -> HorizontalAlignment.JUSTIFY;
            default -> HorizontalAlignment.LEFT;
        };
    }

    private VerticalAlignment verticalAlignment(String value) {
        return switch (value.toLowerCase()) {
            case "top" -> VerticalAlignment.TOP;
            case "bottom" -> VerticalAlignment.BOTTOM;
            default -> VerticalAlignment.CENTER;
        };
    }

    private XSSFColor color(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) {
            return null;
        }
        byte[] rgb = new byte[] {
                (byte) Integer.parseInt(value.substring(1, 3), 16),
                (byte) Integer.parseInt(value.substring(3, 5), 16),
                (byte) Integer.parseInt(value.substring(5, 7), 16)
        };
        return new XSSFColor(rgb, new DefaultIndexedColorMap());
    }

    private XSSFColor colorFromBorder(String value) {
        String[] parts = value.trim().split("\\s+");
        return color(parts.length == 0 ? "" : parts[parts.length - 1]);
    }

    private boolean hasBorder(String value) {
        return !value.isBlank() && !value.toLowerCase().contains("none");
    }
}
