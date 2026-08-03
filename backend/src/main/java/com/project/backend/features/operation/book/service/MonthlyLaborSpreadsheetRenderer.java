package com.project.backend.features.operation.book.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * 月間労務表V1。
 *
 * <p>1従業員・1か月・1シートをA3横1ページ向けの統合表へ配置する。</p>
 */
@Component
@RequiredArgsConstructor
public class MonthlyLaborSpreadsheetRenderer
        implements SpreadsheetLedgerRenderer {

    public static final String KEY = "MONTHLY_LABOR_V1";

    private static final List<String> HEADERS = List.of(
            "日", "曜", "元請", "現場", "勤務時間", "H",
            "通常給金", "早出・残業", "休日", "深夜", "車両代", "その他手当",
            "支払給 A", "税額他", "積立", "返済", "前払い",
            "その他控除", "当日支給額 B", "寮費", "その他", "備考"
    );

    private static final List<Double> WIDTHS = List.of(
            34d, 28d, 82d, 82d, 74d, 38d,
            64d, 64d, 58d, 58d, 58d, 64d, 68d,
            58d, 54d, 54d, 58d, 64d, 68d, 54d, 54d, 90d
    );

    private final ObjectMapper objectMapper;

    @Override
    public String rendererKey() {
        return KEY;
    }

    @Override
    public boolean requiresTemplate() {
        return false;
    }

    @Override
    public JsonNode render(SpreadsheetLedgerRenderContext context) {
        YearMonth targetMonth = YearMonth.parse(context.targetMonth());
        Map<Integer, Map<String, Object>> rowsByDay = new HashMap<>();
        for (Map<String, Object> row : context.sourceRows()) {
            LocalDate workDate = date(row.get("work_date"));
            if (workDate != null) {
                rowsByDay.put(workDate.getDayOfMonth(), row);
            }
        }

        Map<String, Object> employee = context.sourceRows().isEmpty()
                ? Map.of()
                : context.sourceRows().getFirst();
        String employeeCode = text(employee.get("employee_code"));
        String employeeName = text(employee.get("employee_name"));
        String companyName = text(employee.get("company_name"));

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode workbook = root.putObject("Workbook");
        workbook.put("locale", "ja");
        ArrayNode sheets = workbook.putArray("sheets");
        ObjectNode sheet = sheets.addObject();
        sheet.put("name", sheetName(employeeName, targetMonth));
        sheet.put("frozenRows", 3);
        sheet.put("showGridLines", false);
        sheet.putObject("usedRange")
                .put("rowIndex", 34)
                .put("colIndex", HEADERS.size() - 1);

        ArrayNode columns = sheet.putArray("columns");
        for (double width : WIDTHS) {
            columns.addObject().put("width", width);
        }

        ArrayNode rows = sheet.putArray("rows");
        titleRow(
                rows,
                targetMonth,
                employeeCode,
                employeeName,
                companyName
        );
        summaryRow(rows, context.sourceRows());
        headerRow(rows);

        for (int day = 1; day <= 31; day++) {
            LocalDate date = day <= targetMonth.lengthOfMonth()
                    ? targetMonth.atDay(day)
                    : null;
            detailRow(rows, date, rowsByDay.get(day));
        }
        totalRow(rows, context.sourceRows());

        ObjectNode metadata = root.putObject("projectAdminMetadata");
        metadata.put("rendererKey", KEY);
        metadata.put("bookCode", context.master().getBookCode());
        metadata.put("targetMonth", context.targetMonth());
        metadata.put("employeeCode", employeeCode);
        metadata.put("employeeName", employeeName);
        metadata.put("generatedAt", context.generatedAt().toString());
        metadata.put("paperSize", "A3");
        metadata.put("orientation", "LANDSCAPE");
        metadata.put("fitToOnePage", true);
        return root;
    }

    @Override
    public boolean editableBeforeClosing() {
        return true;
    }

    @Override
    public boolean usesStableMonthlyPath() {
        return true;
    }

    private void titleRow(
            ArrayNode rows,
            YearMonth targetMonth,
            String employeeCode,
            String employeeName,
            String companyName
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 28);
        ArrayNode cells = row.putArray("cells");
        ObjectNode title = cells.addObject();
        title.put(
                "value",
                "%d年%02d月分　No.%s　氏名：%s　　月間労務表　　%s".formatted(
                        targetMonth.getYear(),
                        targetMonth.getMonthValue(),
                        employeeCode,
                        employeeName,
                        companyName
                ).trim()
        );
        title.put("colSpan", HEADERS.size());
        title.set("style", style(true, 15, "Center", "#D9EAF7"));
    }

    private void summaryRow(
            ArrayNode rows,
            List<Map<String, Object>> sourceRows
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 22);
        ArrayNode cells = row.putArray("cells");
        ObjectNode cell = cells.addObject();
        cell.put(
                "value",
                "勤務日数 %d日　通常 %s　残業 %s　休日 %s　深夜 %s".formatted(
                        sourceRows.size(),
                        amount(sourceRows, "normal_pay_amount"),
                        amount(sourceRows, "overtime_pay_amount"),
                        amount(sourceRows, "holiday_pay_amount"),
                        amount(sourceRows, "night_pay_amount")
                )
        );
        cell.put("colSpan", HEADERS.size());
        cell.set("style", style(false, 9, "Left", "#F4F7FA"));
    }

    private void headerRow(ArrayNode rows) {
        ObjectNode row = rows.addObject();
        row.put("height", 34);
        ArrayNode cells = row.putArray("cells");
        for (String header : HEADERS) {
            ObjectNode cell = cells.addObject();
            cell.put("value", header);
            cell.set("style", style(true, 8, "Center", "#D9EAD3"));
        }
    }

    private void detailRow(
            ArrayNode rows,
            LocalDate date,
            Map<String, Object> source
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 19);
        ArrayNode cells = row.putArray("cells");
        if (date == null) {
            for (int index = 0; index < HEADERS.size(); index++) {
                blankCell(cells, "#EEEEEE");
            }
            return;
        }

        String weekendColor = weekendColor(date.getDayOfWeek());
        valueCell(cells, date.getDayOfMonth(), "0", weekendColor, "Center");
        valueCell(cells, weekday(date.getDayOfWeek()), null, weekendColor, "Center");
        valueCell(cells, value(source, "customer_name"), null, null, "Left");
        valueCell(cells, value(source, "site_name"), null, null, "Left");
        valueCell(cells, value(source, "work_time"), null, null, "Center");
        valueCell(cells, value(source, "total_hours"), "0.00", null, "Right");
        moneyCells(cells, source,
                "normal_pay_amount",
                "overtime_pay_amount",
                "holiday_pay_amount",
                "night_pay_amount",
                "vehicle_allowance_amount",
                "other_allowance_amount",
                "gross_pay_amount",
                "tax_other_amount",
                "saving_amount",
                "loan_repayment_amount",
                "advance_payment_amount",
                "other_deduction_amount",
                "available_payment_amount",
                "dormitory_fee_amount",
                "other_daily_amount");
        valueCell(cells, value(source, "note"), null, null, "Left");
    }

    private void totalRow(
            ArrayNode rows,
            List<Map<String, Object>> sourceRows
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 22);
        ArrayNode cells = row.putArray("cells");
        ObjectNode label = cells.addObject();
        label.put("value", "合計");
        label.set("style", style(true, 9, "Center", "#FFF2CC"));
        for (int index = 0; index < 4; index++) {
            blankCell(cells, "#FFF2CC");
        }
        valueCell(cells, amount(sourceRows, "total_hours"), "0.00", "#FFF2CC", "Right");
        for (String key : List.of(
                "normal_pay_amount", "overtime_pay_amount",
                "holiday_pay_amount", "night_pay_amount",
                "vehicle_allowance_amount",
                "other_allowance_amount", "gross_pay_amount",
                "tax_other_amount", "saving_amount",
                "loan_repayment_amount", "advance_payment_amount",
                "other_deduction_amount", "available_payment_amount",
                "dormitory_fee_amount", "other_daily_amount"
        )) {
            valueCell(cells, amount(sourceRows, key), "#,##0", "#FFF2CC", "Right");
        }
        blankCell(cells, "#FFF2CC");
    }

    private void moneyCells(
            ArrayNode cells,
            Map<String, Object> source,
            String... keys
    ) {
        for (String key : keys) {
            valueCell(cells, value(source, key), "#,##0", null, "Right");
        }
    }

    private void valueCell(
            ArrayNode cells,
            Object value,
            String format,
            String background,
            String align
    ) {
        ObjectNode cell = cells.addObject();
        if (value != null) {
            if (value instanceof Number number) {
                cell.put("value", number.doubleValue());
            } else {
                cell.put("value", value.toString());
            }
        }
        if (format != null) {
            cell.put("format", format);
        }
        cell.set("style", style(false, 8, align, background));
    }

    private void blankCell(ArrayNode cells, String background) {
        valueCell(cells, null, null, background, "Left");
    }

    private ObjectNode style(
            boolean bold,
            int fontSize,
            String align,
            String background
    ) {
        ObjectNode style = objectMapper.createObjectNode();
        style.put("fontFamily", "Noto Sans JP");
        style.put("fontSize", fontSize + "pt");
        style.put("fontWeight", bold ? "bold" : "normal");
        style.put("textAlign", align);
        style.put("verticalAlign", "middle");
        style.put("border", "1px solid #777777");
        style.put("whiteSpace", "normal");
        if (background != null) {
            style.put("backgroundColor", background);
        }
        return style;
    }

    private Object value(Map<String, Object> source, String key) {
        return source == null ? null : source.get(key);
    }

    private BigDecimal amount(
            List<Map<String, Object>> rows,
            String key
    ) {
        return rows.stream()
                .map(row -> decimal(row.get(key)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private LocalDate date(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return value == null ? null : LocalDate.parse(value.toString());
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String sheetName(String employeeName, YearMonth targetMonth) {
        String name = employeeName.isBlank() ? "従業員" : employeeName;
        String safe = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
        String result = targetMonth + " " + safe;
        return result.length() <= 31 ? result : result.substring(0, 31);
    }

    private String weekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "月";
            case TUESDAY -> "火";
            case WEDNESDAY -> "水";
            case THURSDAY -> "木";
            case FRIDAY -> "金";
            case SATURDAY -> "土";
            case SUNDAY -> "日";
        };
    }

    private String weekendColor(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SATURDAY -> "#DDEBF7";
            case SUNDAY -> "#FCE4D6";
            default -> null;
        };
    }
}
