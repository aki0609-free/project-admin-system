package com.project.backend.features.system.report.service.api.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportOutputFormat;

/**
 * Excelテンプレート内のプレースホルダーを帳票出力行で置換する共通Renderer。
 */
@Component
public class GenericExcelTemplateReportRenderer
        implements ExcelTemplateReportRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "\\$\\{(?:(row)\\.)?([A-Za-z_][A-Za-z0-9_]*)(?::([^}]+))?}"
    );
    private static final Pattern DATE_SHEET_NAME = Pattern.compile(
            "\\d{4}\\.\\d{1,2}"
    );
    private static final CellCopyPolicy ROW_COPY_POLICY =
            new CellCopyPolicy.Builder()
                    .cellValue(true)
                    .cellStyle(true)
                    .cellFormula(true)
                    .copyHyperlink(true)
                    .rowHeight(true)
                    .mergedRegions(true)
                    .build();

    @Override
    public boolean supports(ReportMaster reportMaster) {
        return reportMaster.getOutputFormat() == ReportOutputFormat.EXCEL;
    }

    @Override
    public boolean fallback() {
        return true;
    }

    @Override
    public byte[] render(
            ReportMaster reportMaster,
            byte[] template,
            List<Map<String, Object>> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException(
                    "Excelテンプレートの出力対象がありません。reportCode="
                            + reportMaster.getReportCode()
            );
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(template));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            List<NormalizedRow> normalizedRows = rows.stream()
                    .map(NormalizedRow::new)
                    .toList();
            int placeholderCount = 0;

            for (Sheet rawSheet : workbook) {
                XSSFSheet sheet = (XSSFSheet) rawSheet;
                placeholderCount += renderDetailRows(sheet, normalizedRows);
            }
            for (Sheet sheet : workbook) {
                placeholderCount += replaceScalarPlaceholders(
                        sheet,
                        normalizedRows.getFirst()
                );
            }

            if (placeholderCount == 0) {
                throw new IllegalStateException(
                        "Excelテンプレートにプレースホルダーがありません。reportCode="
                                + reportMaster.getReportCode()
                );
            }

            renameSingleDateSheet(workbook, normalizedRows.getFirst());
            workbook.setForceFormulaRecalculation(true);
            workbook.write(output);
            return output.toByteArray();
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Excelテンプレートの生成に失敗しました。reportCode="
                            + reportMaster.getReportCode(),
                    exception
            );
        }
    }

    private int renderDetailRows(
            XSSFSheet sheet,
            List<NormalizedRow> rows
    ) {
        List<Integer> templateRows = findDetailTemplateRows(sheet);
        if (templateRows.size() > 1) {
            throw new IllegalStateException(
                    "1シートに複数の明細テンプレート行は設定できません。sheet="
                            + sheet.getSheetName()
            );
        }
        if (templateRows.isEmpty()) {
            return 0;
        }

        int templateRowIndex = templateRows.getFirst();
        int footerRowIndex = findFooterRow(sheet, templateRowIndex);
        int capacity = footerRowIndex - templateRowIndex;
        int additionalRows = Math.max(0, rows.size() - capacity);
        if (additionalRows > 0) {
            sheet.shiftRows(
                    footerRowIndex,
                    sheet.getLastRowNum(),
                    additionalRows,
                    true,
                    false
            );
        }

        int actualFooterRowIndex = footerRowIndex + additionalRows;
        for (int index = 1; index < rows.size(); index++) {
            sheet.copyRows(
                    templateRowIndex,
                    templateRowIndex,
                    templateRowIndex + index,
                    ROW_COPY_POLICY
            );
        }

        int replacements = 0;
        for (int index = 0; index < rows.size(); index++) {
            replacements += replaceRowPlaceholders(
                    sheet,
                    sheet.getRow(templateRowIndex + index),
                    rows.get(index)
            );
        }
        clearUnusedDetailRows(
                sheet,
                templateRowIndex + rows.size(),
                actualFooterRowIndex
        );
        adjustFooterFormulas(
                sheet,
                actualFooterRowIndex,
                templateRowIndex + 1,
                footerRowIndex,
                templateRowIndex + rows.size()
        );
        return replacements;
    }

    private List<Integer> findDetailTemplateRows(Sheet sheet) {
        List<Integer> result = new ArrayList<>();
        for (Row row : sheet) {
            boolean detailRow = false;
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING
                        && containsRowPlaceholder(cell.getStringCellValue())) {
                    detailRow = true;
                    break;
                }
            }
            if (detailRow) {
                result.add(row.getRowNum());
            }
        }
        return result;
    }

    private int findFooterRow(Sheet sheet, int templateRowIndex) {
        for (int rowIndex = templateRowIndex + 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && hasFixedValue(row)) {
                return rowIndex;
            }
        }
        return templateRowIndex + 1;
    }

    private boolean hasFixedValue(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue();
                if (value != null && !value.isBlank()
                        && !PLACEHOLDER.matcher(value).find()) {
                    return true;
                }
            } else if (cell.getCellType() == CellType.NUMERIC
                    || cell.getCellType() == CellType.BOOLEAN) {
                return true;
            }
        }
        return false;
    }

    private int replaceRowPlaceholders(
            Sheet sheet,
            Row row,
            NormalizedRow values
    ) {
        int replacements = 0;
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.STRING) {
                continue;
            }
            String original = cell.getStringCellValue();
            Matcher matcher = PLACEHOLDER.matcher(original);
            if (!matcher.find() || matcher.group(1) == null) {
                continue;
            }
            replacements += replaceCell(
                    sheet,
                    cell,
                    original,
                    values,
                    true
            );
        }
        return replacements;
    }

    private int replaceScalarPlaceholders(
            Sheet sheet,
            NormalizedRow values
    ) {
        int replacements = 0;
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String original = cell.getStringCellValue();
                Matcher matcher = PLACEHOLDER.matcher(original);
                if (!matcher.find() || matcher.group(1) != null) {
                    continue;
                }
                replacements += replaceCell(
                        sheet,
                        cell,
                        original,
                        values,
                        false
                );
            }
        }
        return replacements;
    }

    private int replaceCell(
            Sheet sheet,
            Cell cell,
            String original,
            NormalizedRow values,
            boolean rowPlaceholder
    ) {
        Matcher exact = PLACEHOLDER.matcher(original);
        if (exact.matches()) {
            validatePlaceholderKind(exact, rowPlaceholder, sheet, cell);
            Object value = values.require(exact.group(2), sheet, cell);
            setTypedValue(cell, value, exact.group(3));
            return 1;
        }

        Matcher matcher = PLACEHOLDER.matcher(original);
        StringBuffer replaced = new StringBuffer();
        int count = 0;
        while (matcher.find()) {
            if ((matcher.group(1) != null) != rowPlaceholder) {
                continue;
            }
            Object value = values.require(matcher.group(2), sheet, cell);
            matcher.appendReplacement(
                    replaced,
                    Matcher.quoteReplacement(format(value, matcher.group(3)))
            );
            count++;
        }
        if (count > 0) {
            matcher.appendTail(replaced);
            cell.setCellValue(replaced.toString());
        }
        return count;
    }

    private void validatePlaceholderKind(
            Matcher matcher,
            boolean expectedRowPlaceholder,
            Sheet sheet,
            Cell cell
    ) {
        if ((matcher.group(1) != null) != expectedRowPlaceholder) {
            throw new IllegalStateException(
                    "Excelプレースホルダー種別が不正です。sheet="
                            + sheet.getSheetName()
                            + ", cell="
                            + cell.getAddress()
            );
        }
    }

    private void setTypedValue(Cell cell, Object value, String format) {
        if (value == null) {
            cell.setBlank();
        } else if (format != null && !format.isBlank()) {
            cell.setCellValue(format(value, format));
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (toTemporal(value) instanceof TemporalAccessor temporal) {
            cell.setCellValue(toLocalDateTime(temporal));
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private String format(Object value, String pattern) {
        if (value == null) {
            return "";
        }
        if (pattern == null || pattern.isBlank()) {
            return String.valueOf(value);
        }
        TemporalAccessor temporal = toTemporal(value);
        if (temporal != null) {
            return DateTimeFormatter.ofPattern(pattern, Locale.JAPAN)
                    .format(temporal);
        }
        if (value instanceof Number number) {
            return new DecimalFormat(pattern).format(number);
        }
        return String.valueOf(value);
    }

    private TemporalAccessor toTemporal(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof TemporalAccessor temporal) {
            return temporal;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            try {
                if (trimmed.length() == 7) {
                    return YearMonth.parse(trimmed);
                }
                if (trimmed.length() >= 10) {
                    return LocalDate.parse(trimmed.substring(0, 10));
                }
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(TemporalAccessor temporal) {
        if (temporal instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (temporal instanceof LocalDate date) {
            return date.atStartOfDay();
        }
        if (temporal instanceof YearMonth month) {
            return month.atDay(1).atStartOfDay();
        }
        return LocalDate.from(temporal).atStartOfDay();
    }

    private void clearUnusedDetailRows(
            Sheet sheet,
            int startRowIndex,
            int footerRowIndex
    ) {
        for (int rowIndex = startRowIndex;
             rowIndex < footerRowIndex;
             rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                cell.setBlank();
            }
        }
    }

    private void adjustFooterFormulas(
            Sheet sheet,
            int footerRowIndex,
            int firstDetailExcelRow,
            int originalLastDetailExcelRow,
            int actualLastDetailExcelRow
    ) {
        Pattern detailRange = Pattern.compile(
                "(?i)(\\$?[A-Z]{1,3}\\$?"
                        + firstDetailExcelRow
                        + ":\\$?[A-Z]{1,3}\\$?)"
                        + originalLastDetailExcelRow
                        + "\\b"
        );
        for (int rowIndex = footerRowIndex;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.FORMULA) {
                    cell.setCellFormula(replaceDetailRangeEnd(
                            detailRange,
                            cell.getCellFormula(),
                            actualLastDetailExcelRow
                    ));
                }
            }
        }
    }

    private String replaceDetailRangeEnd(
            Pattern detailRange,
            String formula,
            int actualLastDetailExcelRow
    ) {
        Matcher matcher = detailRange.matcher(formula);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(
                            matcher.group(1) + actualLastDetailExcelRow
                    )
            );
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private boolean containsRowPlaceholder(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                return true;
            }
        }
        return false;
    }

    private void renameSingleDateSheet(
            XSSFWorkbook workbook,
            NormalizedRow row
    ) {
        if (workbook.getNumberOfSheets() != 1
                || !DATE_SHEET_NAME.matcher(
                        workbook.getSheetName(0)
                ).matches()) {
            return;
        }
        Object targetMonth = row.find("target_month");
        TemporalAccessor temporal = toTemporal(targetMonth);
        if (temporal == null) {
            return;
        }
        workbook.setSheetName(
                0,
                DateTimeFormatter.ofPattern("yyyy.M").format(temporal)
        );
    }

    private static final class NormalizedRow {

        private final Map<String, Object> values = new HashMap<>();

        private NormalizedRow(Map<String, Object> source) {
            source.forEach((key, value) -> {
                values.put(normalize(key), value);
                values.put(normalize(toSnakeCase(key)), value);
            });
        }

        private Object require(String key, Sheet sheet, Cell cell) {
            String normalized = normalize(key);
            if (!values.containsKey(normalized)) {
                throw new IllegalStateException(
                        "Excelテンプレートの列が出力データに存在しません。key="
                                + key
                                + ", sheet="
                                + sheet.getSheetName()
                                + ", cell="
                                + cell.getAddress()
                );
            }
            return values.get(normalized);
        }

        private Object find(String key) {
            return values.get(normalize(key));
        }

        private static String normalize(String value) {
            return value == null
                    ? ""
                    : value.replace("_", "").toLowerCase(Locale.ROOT);
        }

        private static String toSnakeCase(String value) {
            return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        }
    }
}
