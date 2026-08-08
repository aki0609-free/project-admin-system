package com.project.backend.features.operation.book.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * 労務費支払一覧V1。
 *
 * <p>支払周期ごとに従業員を列、日付を行として配置する。
 * 印刷時の可読性を維持するため、1シートは最大10名とする。</p>
 */
@Component
@RequiredArgsConstructor
public class LaborCostPaymentSpreadsheetRenderer
        implements SpreadsheetLedgerRenderer {

    public static final String KEY = "LABOR_COST_PAYMENT_V1";
    private static final int EMPLOYEES_PER_SHEET = 10;
    private static final List<String> PAYMENT_CYCLES = List.of(
            "DAILY", "WEEKLY", "MONTHLY"
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
    public boolean editableBeforeClosing() {
        return true;
    }

    @Override
    public boolean usesStableMonthlyPath() {
        return true;
    }

    @Override
    public JsonNode render(SpreadsheetLedgerRenderContext context) {
        YearMonth targetMonth = YearMonth.parse(context.targetMonth());
        Map<String, List<EmployeePayment>> grouped = groupEmployees(
                context.sourceRows()
        );

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode workbook = root.putObject("Workbook");
        workbook.put("locale", "ja");
        ArrayNode sheets = workbook.putArray("sheets");

        for (String paymentCycle : PAYMENT_CYCLES) {
            List<EmployeePayment> employees = grouped.getOrDefault(
                    paymentCycle,
                    List.of()
            );
            for (int offset = 0;
                    offset < employees.size();
                    offset += EMPLOYEES_PER_SHEET) {
                int end = Math.min(
                        offset + EMPLOYEES_PER_SHEET,
                        employees.size()
                );
                addSheet(
                        sheets,
                        targetMonth,
                        paymentCycle,
                        employees.subList(offset, end),
                        offset / EMPLOYEES_PER_SHEET + 1
                );
            }
        }

        if (sheets.isEmpty()) {
            // 対象データがなくても、画面で帳票フォーマットを確認できる
            // ように月払い用の空フォームを生成する。
            addSheet(
                    sheets,
                    targetMonth,
                    "MONTHLY",
                    emptyEmployees(),
                    1
            );
        }

        ObjectNode metadata = root.putObject("projectAdminMetadata");
        metadata.put("rendererKey", KEY);
        metadata.put("bookCode", context.master().getBookCode());
        metadata.put("targetMonth", context.targetMonth());
        metadata.put("generatedAt", context.generatedAt().toString());
        metadata.put("paperSize", "A4");
        metadata.put("orientation", "LANDSCAPE");
        metadata.put("fitToOnePage", true);
        metadata.put("employeesPerSheet", EMPLOYEES_PER_SHEET);
        metadata.put("sheetCount", sheets.size());
        return root;
    }

    private Map<String, List<EmployeePayment>> groupEmployees(
            List<Map<String, Object>> sourceRows
    ) {
        Map<EmployeeKey, EmployeePayment> employees = new LinkedHashMap<>();
        for (Map<String, Object> row : sourceRows) {
            String paymentCycle = paymentCycle(row.get("payment_cycle"));
            EmployeeKey key = new EmployeeKey(
                    paymentCycle,
                    text(row.get("employee_id")),
                    text(row.get("employee_code")),
                    text(row.get("employee_name"))
            );
            employees.computeIfAbsent(
                    key,
                    ignored -> new EmployeePayment(
                            key,
                            text(row.get("company_name"))
                    )
            ).add(row);
        }

        Map<String, List<EmployeePayment>> grouped = new LinkedHashMap<>();
        employees.values().stream()
                .sorted(Comparator
                        .comparing((EmployeePayment value) ->
                                PAYMENT_CYCLES.indexOf(
                                        value.key.paymentCycle
                                ))
                        .thenComparing(value -> value.key.employeeCode)
                        .thenComparing(value -> value.key.employeeId))
                .forEach(employee -> grouped
                        .computeIfAbsent(
                                employee.key.paymentCycle,
                                ignored -> new ArrayList<>()
                        )
                        .add(employee));
        return grouped;
    }

    private void addSheet(
            ArrayNode sheets,
            YearMonth targetMonth,
            String paymentCycle,
            List<EmployeePayment> employees,
            int page
    ) {
        int totalColumn = employees.size() + 2;
        ObjectNode sheet = sheets.addObject();
        String label = paymentCycleLabel(paymentCycle);
        sheet.put("name", page == 1 ? label : label + " " + page);
        sheet.put("frozenRows", 5);
        sheet.put("frozenColumns", 2);
        sheet.put("showGridLines", false);
        sheet.putObject("usedRange")
                .put("rowIndex", 44)
                .put("colIndex", totalColumn);

        ArrayNode columns = sheet.putArray("columns");
        columns.addObject().put("width", 42);
        columns.addObject().put("width", 32);
        for (int index = 0; index < employees.size(); index++) {
            columns.addObject().put("width", 88);
        }
        columns.addObject().put("width", 92);

        ArrayNode rows = sheet.putArray("rows");
        titleRow(rows, targetMonth, label, employees, totalColumn + 1);
        employeeHeaderRow(rows, "名前", employees,
                value -> value.key.employeeName, totalColumn);
        employeeHeaderRow(rows, "従業員番号", employees,
                value -> value.key.employeeCode, totalColumn);
        employeeHeaderRow(rows, "支払い方法", employees,
                value -> label, totalColumn);
        dateHeaderRow(rows, employees.size());

        for (int day = 1; day <= 31; day++) {
            LocalDate date = day <= targetMonth.lengthOfMonth()
                    ? targetMonth.atDay(day)
                    : null;
            dailyRow(rows, date, employees);
        }

        summaryRow(rows, "月合計", employees,
                EmployeePayment::grossTotal, "#FFF2CC");
        spacerRow(rows, totalColumn + 1);
        summaryRow(rows, "所得税", employees,
                EmployeePayment::incomeTaxTotal, "#FCE4D6");
        summaryRow(rows, "借金・引出", employees,
                EmployeePayment::borrowWithdrawalAmount, "#FCE4D6");
        summaryRow(rows, "貯金・返済", employees,
                EmployeePayment::savingRepaymentTotal, "#FCE4D6");
        summaryRow(rows, "その他控除", employees,
                EmployeePayment::otherDeductionTotal, "#FCE4D6");
        summaryRow(rows, "控除合計", employees,
                EmployeePayment::deductionTotal, "#F4CCCC");
        spacerRow(rows, totalColumn + 1);
        summaryRow(rows, "差引支給額", employees,
                EmployeePayment::netAmount, "#D9EAD3");
    }

    private void titleRow(
            ArrayNode rows,
            YearMonth targetMonth,
            String paymentCycleLabel,
            List<EmployeePayment> employees,
            int columnCount
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 30);
        ArrayNode cells = row.putArray("cells");
        ObjectNode cell = cells.addObject();
        String companyName = employees.stream()
                .map(EmployeePayment::companyName)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
        cell.put(
                "value",
                "%d年%02d月分　労務費支払一覧総支給（%s）　%s".formatted(
                        targetMonth.getYear(),
                        targetMonth.getMonthValue(),
                        paymentCycleLabel,
                        companyName
                ).trim()
        );
        cell.put("colSpan", columnCount);
        cell.set("style", style(true, 15, "Center", "#D9EAF7"));
    }

    private void employeeHeaderRow(
            ArrayNode rows,
            String label,
            List<EmployeePayment> employees,
            java.util.function.Function<EmployeePayment, String> value,
            int totalColumn
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 21);
        ArrayNode cells = row.putArray("cells");
        ObjectNode header = cells.addObject();
        header.put("value", label);
        header.put("colSpan", 2);
        header.set("style", style(true, 8, "Center", "#EAF2F8"));
        for (int index = 0; index < employees.size(); index++) {
            ObjectNode cell = valueCell(
                    cells,
                    value.apply(employees.get(index)),
                    null,
                    "#EAF2F8",
                    "Center",
                    true
            );
            cell.put("index", index + 2);
        }
        ObjectNode total = valueCell(
                cells,
                "合計",
                null,
                "#EAF2F8",
                "Center",
                true
        );
        total.put("index", totalColumn);
    }

    private void dateHeaderRow(ArrayNode rows, int employeeCount) {
        ObjectNode row = rows.addObject();
        row.put("height", 22);
        ArrayNode cells = row.putArray("cells");
        valueCell(cells, "日付", null, "#D9EAD3", "Center", true);
        valueCell(cells, "曜日", null, "#D9EAD3", "Center", true);
        for (int index = 0; index < employeeCount; index++) {
            valueCell(cells, "総支給", null, "#D9EAD3", "Center", true);
        }
        valueCell(cells, "合計", null, "#D9EAD3", "Center", true);
    }

    private void dailyRow(
            ArrayNode rows,
            LocalDate date,
            List<EmployeePayment> employees
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 18);
        ArrayNode cells = row.putArray("cells");
        if (date == null) {
            for (int index = 0; index < employees.size() + 3; index++) {
                valueCell(cells, null, null, "#EEEEEE", "Right", false);
            }
            return;
        }

        String background = weekendColor(date.getDayOfWeek());
        valueCell(cells, date.getDayOfMonth() + "日", null,
                background, "Center", false);
        valueCell(cells, weekday(date.getDayOfWeek()), null,
                background, "Center", false);
        BigDecimal total = BigDecimal.ZERO;
        for (EmployeePayment employee : employees) {
            BigDecimal amount = employee.dailyAmounts.getOrDefault(
                    date.getDayOfMonth(),
                    BigDecimal.ZERO
            );
            valueCell(cells, amount, "#,##0", background, "Right", false);
            total = total.add(amount);
        }
        valueCell(cells, total, "#,##0", background, "Right", true);
    }

    private void summaryRow(
            ArrayNode rows,
            String label,
            List<EmployeePayment> employees,
            java.util.function.Function<EmployeePayment, BigDecimal> amount,
            String background
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 20);
        ArrayNode cells = row.putArray("cells");
        ObjectNode header = cells.addObject();
        header.put("value", label);
        header.put("colSpan", 2);
        header.set("style", style(true, 8, "Center", background));

        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < employees.size(); index++) {
            BigDecimal value = amount.apply(employees.get(index));
            ObjectNode cell = valueCell(
                    cells,
                    value,
                    "#,##0",
                    background,
                    "Right",
                    false
            );
            cell.put("index", index + 2);
            total = total.add(value);
        }
        ObjectNode totalCell = valueCell(
                cells,
                total,
                "#,##0",
                background,
                "Right",
                true
        );
        totalCell.put("index", employees.size() + 2);
    }

    private void spacerRow(ArrayNode rows, int columnCount) {
        ObjectNode row = rows.addObject();
        row.put("height", 7);
        ArrayNode cells = row.putArray("cells");
        ObjectNode cell = cells.addObject();
        cell.put("colSpan", columnCount);
        cell.set("style", style(false, 8, "Left", "#FFFFFF"));
    }

    private List<EmployeePayment> emptyEmployees() {
        List<EmployeePayment> employees = new ArrayList<>();
        for (int index = 0; index < EMPLOYEES_PER_SHEET; index++) {
            employees.add(new EmployeePayment(
                    new EmployeeKey("MONTHLY", "", "", ""),
                    ""
            ));
        }
        return employees;
    }

    private ObjectNode valueCell(
            ArrayNode cells,
            Object value,
            String format,
            String background,
            String align,
            boolean bold
    ) {
        ObjectNode cell = cells.addObject();
        if (value instanceof BigDecimal decimal) {
            cell.put("value", decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.put("value", number.doubleValue());
        } else if (value != null) {
            cell.put("value", value.toString());
        }
        if (format != null) {
            cell.put("format", format);
        }
        cell.set("style", style(bold, 8, align, background));
        return cell;
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
        if (background != null) {
            style.put("backgroundColor", background);
        }
        return style;
    }

    private String paymentCycle(Object value) {
        String cycle = text(value).toUpperCase(java.util.Locale.ROOT);
        return PAYMENT_CYCLES.contains(cycle) ? cycle : "MONTHLY";
    }

    private String paymentCycleLabel(String paymentCycle) {
        return switch (paymentCycle) {
            case "DAILY" -> "日払い";
            case "WEEKLY" -> "週払い";
            default -> "月払い";
        };
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

    private LocalDate date(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return value == null ? null : LocalDate.parse(value.toString());
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

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private record EmployeeKey(
            String paymentCycle,
            String employeeId,
            String employeeCode,
            String employeeName
    ) {
    }

    private final class EmployeePayment {
        private final EmployeeKey key;
        private final String companyName;
        private final Map<Integer, BigDecimal> dailyAmounts =
                new LinkedHashMap<>();
        private BigDecimal incomeTaxTotal = BigDecimal.ZERO;
        private BigDecimal savingRepaymentTotal = BigDecimal.ZERO;
        private BigDecimal otherDeductionTotal = BigDecimal.ZERO;
        private BigDecimal borrowWithdrawalAmount = BigDecimal.ZERO;

        private EmployeePayment(EmployeeKey key, String companyName) {
            this.key = key;
            this.companyName = companyName;
        }

        private void add(Map<String, Object> row) {
            LocalDate workDate = date(row.get("work_date"));
            if (workDate != null) {
                dailyAmounts.merge(
                        workDate.getDayOfMonth(),
                        decimal(row.get("daily_gross_amount")),
                        BigDecimal::add
                );
            }
            incomeTaxTotal = incomeTaxTotal.add(
                    decimal(row.get("income_tax_amount"))
            );
            savingRepaymentTotal = savingRepaymentTotal.add(
                    decimal(row.get("saving_repayment_amount"))
            );
            otherDeductionTotal = otherDeductionTotal.add(
                    decimal(row.get("other_deduction_amount"))
            );
            borrowWithdrawalAmount = decimal(
                    row.get("borrow_withdrawal_amount")
            );
        }

        private String companyName() {
            return companyName;
        }

        private BigDecimal grossTotal() {
            return dailyAmounts.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal incomeTaxTotal() {
            return incomeTaxTotal;
        }

        private BigDecimal savingRepaymentTotal() {
            return savingRepaymentTotal;
        }

        private BigDecimal otherDeductionTotal() {
            return otherDeductionTotal;
        }

        private BigDecimal borrowWithdrawalAmount() {
            return borrowWithdrawalAmount;
        }

        private BigDecimal deductionTotal() {
            return incomeTaxTotal
                    .add(borrowWithdrawalAmount)
                    .add(savingRepaymentTotal)
                    .add(otherDeductionTotal);
        }

        private BigDecimal netAmount() {
            return grossTotal().subtract(deductionTotal());
        }
    }
}
