package com.project.backend.features.system.report.service.api.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.entity.ReportMaster;

/**
 * 労務費一覧表の原本レイアウトへ、月次締めで確定した値を差し込む。
 *
 * <p>テンプレート読込・S3保存・履歴管理は帳票共通基盤が担当し、
 * 本クラスは帳票固有のセル配置だけを担当する。</p>
 */
@Component
public class MonthlyLaborCostListExcelRenderer
        implements ExcelTemplateReportRenderer {

    static final String REPORT_CODE = "MONTHLY_LABOR_COST_LIST";
    private static final int DETAIL_START_ROW = 3;
    private static final int TEMPLATE_DETAIL_END_ROW = 35;
    private static final int TEMPLATE_TOTAL_ROW = 36;
    private static final int LAST_OUTPUT_COLUMN = 29;

    private static final Map<Integer, String> DETAIL_COLUMNS = columns();

    @Override
    public boolean supports(ReportMaster reportMaster) {
        return REPORT_CODE.equals(reportMaster.getReportCode());
    }

    @Override
    public byte[] render(
            ReportMaster reportMaster,
            byte[] template,
            List<Map<String, Object>> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("労務費一覧表の出力対象がありません。");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(template));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRowIndex = ensureDetailCapacity(sheet, rows.size());

            writeHeader(workbook, sheet, rows.getFirst());
            writeDetails(sheet, rows, totalRowIndex);
            writeTotals(sheet, totalRowIndex);

            workbook.setPrintArea(
                    workbook.getSheetIndex(sheet),
                    0,
                    LAST_OUTPUT_COLUMN,
                    0,
                    totalRowIndex
            );
            workbook.setForceFormulaRecalculation(true);
            workbook.getCreationHelper()
                    .createFormulaEvaluator()
                    .evaluateAll();
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "労務費一覧表のExcel生成に失敗しました。",
                    exception
            );
        }
    }

    private int ensureDetailCapacity(Sheet sheet, int rowCount) {
        int templateCapacity = TEMPLATE_DETAIL_END_ROW - DETAIL_START_ROW + 1;
        int additionalRows = Math.max(0, rowCount - templateCapacity);
        if (additionalRows == 0) {
            return TEMPLATE_TOTAL_ROW;
        }

        sheet.shiftRows(
                TEMPLATE_TOTAL_ROW,
                sheet.getLastRowNum(),
                additionalRows,
                true,
                false
        );
        Row styleSource = sheet.getRow(TEMPLATE_DETAIL_END_ROW);
        for (int index = 0; index < additionalRows; index++) {
            int rowIndex = TEMPLATE_TOTAL_ROW + index;
            copyRowStyle(styleSource, sheet.createRow(rowIndex));
        }
        return TEMPLATE_TOTAL_ROW + additionalRows;
    }

    private void copyRowStyle(Row source, Row target) {
        target.setHeight(source.getHeight());
        for (int column = 0; column <= LAST_OUTPUT_COLUMN; column++) {
            Cell sourceCell = source.getCell(
                    column,
                    Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
            );
            Cell targetCell = target.createCell(column, CellType.BLANK);
            targetCell.setCellStyle(sourceCell.getCellStyle());
        }
    }

    private void writeHeader(
            Workbook workbook,
            Sheet sheet,
            Map<String, Object> firstRow
    ) {
        LocalDate targetMonthDate = toLocalDate(firstRow.get("target_month"));
        YearMonth targetMonth = YearMonth.from(targetMonthDate);
        LocalDate paymentDate = toLocalDate(firstRow.get("payment_date"));

        setString(sheet, 0, 5, targetMonth.getYear() + "年");
        setString(sheet, 0, 6, targetMonth.getMonthValue() + "月分");
        setString(
                sheet,
                0,
                7,
                paymentDate.getMonthValue()
                        + "/"
                        + paymentDate.getDayOfMonth()
                        + "支払"
        );
        setString(sheet, 0, 26, value(firstRow, "company_name"));

        String sheetName = targetMonth.format(
                DateTimeFormatter.ofPattern("yyyy.M")
        );
        workbook.setSheetName(workbook.getSheetIndex(sheet), sheetName);
    }

    private void writeDetails(
            Sheet sheet,
            List<Map<String, Object>> rows,
            int totalRowIndex
    ) {
        for (int rowIndex = DETAIL_START_ROW;
             rowIndex < totalRowIndex;
             rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            clearRow(row);
        }

        for (int index = 0; index < rows.size(); index++) {
            Row outputRow = sheet.getRow(DETAIL_START_ROW + index);
            Map<String, Object> values = rows.get(index);
            for (Map.Entry<Integer, String> mapping : DETAIL_COLUMNS.entrySet()) {
                setCell(outputRow, mapping.getKey(), values.get(mapping.getValue()));
            }
        }
    }

    private void clearRow(Row row) {
        for (int column = 0; column <= LAST_OUTPUT_COLUMN; column++) {
            Cell cell = row.getCell(
                    column,
                    Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
            );
            cell.setBlank();
        }
    }

    private void writeTotals(Sheet sheet, int totalRowIndex) {
        Row totalRow = sheet.getRow(totalRowIndex);
        if (totalRow == null) {
            totalRow = sheet.createRow(totalRowIndex);
        }
        setCell(totalRow, 0, "計");

        int firstExcelRow = DETAIL_START_ROW + 1;
        int lastExcelRow = totalRowIndex;
        for (int column = 1; column <= LAST_OUTPUT_COLUMN; column++) {
            Cell cell = totalRow.getCell(
                    column,
                    Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
            );
            String columnName = org.apache.poi.ss.util.CellReference
                    .convertNumToColString(column);
            cell.setCellFormula(
                    "SUM("
                            + columnName
                            + firstExcelRow
                            + ":"
                            + columnName
                            + lastExcelRow
                            + ")"
            );
        }
    }

    private void setString(Sheet sheet, int row, int column, String value) {
        setCell(
                sheet.getRow(row),
                column,
                value
        );
    }

    private void setCell(Row row, int column, Object value) {
        Cell cell = row.getCell(
                column,
                Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
        );
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
            return;
        }
        cell.setCellValue(String.valueOf(value));
    }

    private String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value == null) {
            throw new IllegalStateException("帳票日付が未設定です。");
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 7) {
            return YearMonth.parse(text).atDay(1);
        }
        return LocalDate.parse(text.substring(0, 10));
    }

    private static Map<Integer, String> columns() {
        Map<Integer, String> result = new LinkedHashMap<>();
        result.put(0, "employee_name");
        result.put(1, "work_day_count");
        result.put(2, "paid_leave_days");
        result.put(3, "overtime_hours");
        result.put(4, "night_work_hours");
        result.put(5, "basic_salary");
        result.put(6, "overtime_pay_amount");
        result.put(7, "night_pay_amount");
        result.put(8, "driver_allowance_amount");
        result.put(9, "other_allowance_amount");
        result.put(10, "business_trip_allowance_amount");
        result.put(11, "gross_amount");
        result.put(12, "health_insurance");
        result.put(13, "child_care_contribution");
        result.put(14, "pension_insurance");
        result.put(15, "employment_insurance");
        result.put(16, "social_insurance_total");
        result.put(17, "taxable_amount");
        result.put(18, "income_tax");
        result.put(19, "year_end_adjustment_amount");
        result.put(20, "resident_tax");
        result.put(21, "dormitory_fee_amount");
        result.put(22, "mobile_rental_amount");
        result.put(23, "wifi_fee_amount");
        result.put(24, "other_deduction_amount");
        result.put(25, "deduction_total");
        result.put(26, "net_before_advance_amount");
        result.put(27, "advance_payment_amount");
        result.put(28, "saving_amount");
        result.put(29, "net_payment_amount");
        return Map.copyOf(result);
    }
}
