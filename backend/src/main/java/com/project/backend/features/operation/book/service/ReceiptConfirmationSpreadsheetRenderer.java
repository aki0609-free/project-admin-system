package com.project.backend.features.operation.book.service;

import java.math.BigDecimal;
import java.sql.Date;
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
 * 入金確認表V1。
 *
 * <p>S3上のSpreadsheetテンプレートの外観を基準に、
 * customer_transactionsの最新状態を行へ展開する。</p>
 */
@Component
@RequiredArgsConstructor
public class ReceiptConfirmationSpreadsheetRenderer
        implements SpreadsheetLedgerRenderer {

    public static final String KEY = "RECEIPT_CONFIRMATION_V1";

    static final int PAID_AMOUNT_COLUMN = 10;
    static final int BILLING_AMOUNT_COLUMN = 8;
    static final int FEE_COLUMN = 11;
    static final int OFFSET_COLUMN = 12;
    static final int TOTAL_COLUMN = 13;
    static final int NOTE_COLUMN = 14;
    static final int TRANSACTION_ID_COLUMN = 15;
    static final int CUSTOMER_ID_COLUMN = 16;

    private final ObjectMapper objectMapper;

    @Override
    public String rendererKey() {
        return KEY;
    }

    @Override
    public boolean editableBeforeClosing() {
        return true;
    }

    @Override
    public boolean editableAfterMonthlyClosing() {
        return true;
    }

    @Override
    public boolean usesStableMonthlyPath() {
        return true;
    }

    @Override
    public JsonNode render(SpreadsheetLedgerRenderContext context) {
        YearMonth targetMonth = YearMonth.parse(context.targetMonth());
        if (!(context.template() instanceof ObjectNode templateRoot)) {
            throw new IllegalArgumentException(
                    "入金確認表テンプレートが未設定です。"
            );
        }
        ObjectNode root = templateRoot.deepCopy();
        ObjectNode workbook = workbook(root);
        workbook.put("locale", "ja");
        ObjectNode sheet = firstSheet(workbook);
        ArrayNode templateRows = sheet.withArray("rows").deepCopy();
        sheet.put("name", targetMonth.toString());
        sheet.put("showGridLines", false);
        sheet.put("frozenRows", 3);
        sheet.put("isProtected", true);
        ObjectNode protectSettings = sheet.putObject("protectSettings");
        protectSettings.put("selectCells", true);
        protectSettings.put("formatCells", false);
        protectSettings.put("formatRows", false);
        protectSettings.put("formatColumns", false);
        protectSettings.put("insertLink", false);

        ensureHiddenMetadataColumns(sheet);
        ArrayNode rows = objectMapper.createArrayNode();
        titleRow(rows, templateRows, targetMonth, context.sourceRows());
        blankRow(rows);
        headerRow(rows, templateRows);

        List<ReceiptRow> receipts = context.sourceRows().stream()
                .map(this::receiptRow)
                .sorted(Comparator
                        .comparing(
                                ReceiptRow::expectedPaymentDate,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                        .thenComparing(ReceiptRow::customerName)
                        .thenComparing(ReceiptRow::transactionId))
                .toList();

        Map<String, List<ReceiptRow>> groups = groupByExpectedMonth(
                receipts
        );
        List<Integer> detailExcelRows = new ArrayList<>();
        for (Map.Entry<String, List<ReceiptRow>> entry : groups.entrySet()) {
            int firstDetailExcelRow = rows.size() + 1;
            for (ReceiptRow receipt : entry.getValue()) {
                detailExcelRows.add(rows.size() + 1);
                detailRow(rows, templateRows, receipt);
            }
            int lastDetailExcelRow = rows.size();
            subtotalRow(
                    rows,
                    templateRows,
                    entry.getKey(),
                    firstDetailExcelRow,
                    lastDetailExcelRow
            );
        }
        grandTotalRow(rows, templateRows, detailExcelRows);

        sheet.set("rows", rows);
        sheet.putObject("usedRange")
                .put("rowIndex", Math.max(0, rows.size() - 1))
                .put("colIndex", NOTE_COLUMN);

        ObjectNode metadata = root.withObject("/projectAdminMetadata");
        metadata.put("rendererKey", KEY);
        metadata.put("bookCode", context.master().getBookCode());
        metadata.put("targetMonth", context.targetMonth());
        metadata.put("generatedAt", context.generatedAt().toString());
        metadata.put("paperSize", "A4");
        metadata.put("orientation", "LANDSCAPE");
        metadata.put("fitToOnePage", true);
        metadata.put("transactionCount", receipts.size());
        return root;
    }

    private ObjectNode workbook(ObjectNode root) {
        JsonNode nested = root.get("Workbook");
        if (nested instanceof ObjectNode object) {
            return object;
        }
        return root;
    }

    private ObjectNode firstSheet(ObjectNode workbook) {
        ArrayNode sheets = workbook.withArray("sheets");
        if (sheets.isEmpty()) {
            return sheets.addObject();
        }
        if (!(sheets.get(0) instanceof ObjectNode sheet)) {
            throw new IllegalArgumentException(
                    "入金確認表テンプレートの先頭シートが不正です。"
            );
        }
        while (sheets.size() > 1) {
            sheets.remove(sheets.size() - 1);
        }
        return sheet;
    }

    private void ensureHiddenMetadataColumns(ObjectNode sheet) {
        ArrayNode columns = sheet.withArray("columns");
        while (columns.size() <= CUSTOMER_ID_COLUMN) {
            columns.addObject();
        }
        ((ObjectNode) columns.get(TRANSACTION_ID_COLUMN))
                .put("hidden", true);
        ((ObjectNode) columns.get(CUSTOMER_ID_COLUMN))
                .put("hidden", true);
    }

    private void titleRow(
            ArrayNode rows,
            ArrayNode templateRows,
            YearMonth targetMonth,
            List<Map<String, Object>> sourceRows
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 28);
        ArrayNode cells = row.putArray("cells");
        cell(cells, 0, "入金確認表", templateStyle(templateRows, 0, 1), 8);
        cell(
                cells,
                8,
                companyName(sourceRows),
                templateStyle(templateRows, 0, 10),
                3
        );
        cell(
                cells,
                11,
                "%d年度 %d月分".formatted(
                        targetMonth.getYear(),
                        targetMonth.getMonthValue()
                ),
                templateStyle(templateRows, 0, 13),
                4
        );
    }

    private void blankRow(ArrayNode rows) {
        ObjectNode row = rows.addObject();
        row.put("height", 8);
        row.putArray("cells");
    }

    private void headerRow(ArrayNode rows, ArrayNode templateRows) {
        ObjectNode row = rows.addObject();
        row.put("height", 26);
        ArrayNode cells = row.putArray("cells");
        cell(cells, 0, "業者名", templateStyle(templateRows, 2, 0), 2);
        cell(cells, 2, "締め日", templateStyle(templateRows, 2, 2), 3);
        cell(cells, 5, "支払日", templateStyle(templateRows, 2, 5), 3);
        cell(cells, 8, "請求金額", templateStyle(templateRows, 2, 8), 1);
        cell(cells, 9, "入金予定日", templateStyle(templateRows, 2, 9), 1);
        cell(cells, 10, "入金額", templateStyle(templateRows, 2, 10), 1);
        cell(cells, 11, "手数料", templateStyle(templateRows, 2, 11), 1);
        cell(cells, 12, "相殺", templateStyle(templateRows, 2, 12), 1);
        cell(cells, 13, "合計金額", templateStyle(templateRows, 2, 13), 1);
        cell(cells, 14, "備考（相殺内容等）",
                templateStyle(templateRows, 2, 14), 1);
    }

    private void detailRow(
            ArrayNode rows,
            ArrayNode templateRows,
            ReceiptRow receipt
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 23);
        ArrayNode cells = row.putArray("cells");
        String background = statusColor(receipt.paymentStatus());

        ObjectNode name = cell(
                cells,
                0,
                receipt.customerName(),
                detailStyle(templateRows, 3, 0, background),
                2
        );
        name.put("isLocked", true);
        lockedCell(cells, 2, receipt.closingRuleText(),
                detailStyle(templateRows, 3, 2, background), 3);
        lockedCell(cells, 5, receipt.paymentRuleText(),
                detailStyle(templateRows, 3, 5, background), 3);
        lockedMoney(cells, 8, receipt.billingAmount(),
                detailStyle(templateRows, 3, 8, background));
        lockedCell(cells, 9, dateText(receipt.expectedPaymentDate()),
                detailStyle(templateRows, 3, 9, background), 1);
        editableMoney(cells, PAID_AMOUNT_COLUMN, receipt.paidAmount(),
                detailStyle(templateRows, 3, 10, background));
        editableMoney(cells, FEE_COLUMN, receipt.fee(),
                detailStyle(templateRows, 3, 11, background));
        editableMoney(cells, OFFSET_COLUMN, receipt.offsetAmount(),
                detailStyle(templateRows, 3, 12, background));

        ObjectNode total = lockedMoney(
                cells,
                TOTAL_COLUMN,
                receipt.settledAmount(),
                detailStyle(templateRows, 3, 13, background)
        );
        int excelRow = rows.size();
        total.put("formula", "=SUM(K%d:M%d)".formatted(
                excelRow,
                excelRow
        ));

        ObjectNode note = cell(
                cells,
                NOTE_COLUMN,
                receipt.note(),
                detailStyle(templateRows, 3, 14, background),
                1
        );
        note.put("isLocked", false);
        lockedCell(cells, TRANSACTION_ID_COLUMN,
                receipt.transactionId(), objectMapper.createObjectNode(), 1);
        lockedCell(cells, CUSTOMER_ID_COLUMN,
                receipt.customerId(), objectMapper.createObjectNode(), 1);
    }

    private void subtotalRow(
            ArrayNode rows,
            ArrayNode templateRows,
            String groupLabel,
            int firstExcelRow,
            int lastExcelRow
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 23);
        ArrayNode cells = row.putArray("cells");
        String background = "#D9E1F2";
        lockedCell(
                cells,
                0,
                groupLabel + " 合計",
                detailStyle(templateRows, 9, 0, background),
                8
        );
        for (int column : List.of(8, 10, 11, 12, 13)) {
            ObjectNode total = lockedMoney(
                    cells,
                    column,
                    BigDecimal.ZERO,
                    detailStyle(templateRows, 9, column, background)
            );
            total.put(
                    "formula",
                    "=SUM(%s%d:%s%d)".formatted(
                            columnName(column),
                            firstExcelRow,
                            columnName(column),
                            lastExcelRow
                    )
            );
        }
    }

    private void grandTotalRow(
            ArrayNode rows,
            ArrayNode templateRows,
            List<Integer> detailExcelRows
    ) {
        ObjectNode row = rows.addObject();
        row.put("height", 25);
        ArrayNode cells = row.putArray("cells");
        String background = "#C9C9F5";
        lockedCell(
                cells,
                0,
                "総合計",
                detailStyle(templateRows, 13, 0, background),
                8
        );
        for (int column : List.of(8, 10, 11, 12, 13)) {
            ObjectNode total = lockedMoney(
                    cells,
                    column,
                    BigDecimal.ZERO,
                    detailStyle(templateRows, 13, column, background)
            );
            total.put("formula", sumFormula(column, detailExcelRows));
        }
    }

    private ObjectNode editableMoney(
            ArrayNode cells,
            int index,
            BigDecimal value,
            ObjectNode style
    ) {
        ObjectNode cell = cell(cells, index, value, style, 1);
        cell.put("format", "#,##0");
        cell.put("isLocked", false);
        return cell;
    }

    private ObjectNode lockedMoney(
            ArrayNode cells,
            int index,
            BigDecimal value,
            ObjectNode style
    ) {
        ObjectNode cell = lockedCell(cells, index, value, style, 1);
        cell.put("format", "#,##0");
        return cell;
    }

    private ObjectNode lockedCell(
            ArrayNode cells,
            int index,
            Object value,
            ObjectNode style,
            int colSpan
    ) {
        ObjectNode cell = cell(cells, index, value, style, colSpan);
        cell.put("isLocked", true);
        return cell;
    }

    private ObjectNode cell(
            ArrayNode cells,
            int index,
            Object value,
            ObjectNode style,
            int colSpan
    ) {
        ObjectNode cell = cells.addObject();
        cell.put("index", index);
        if (value instanceof BigDecimal decimal) {
            cell.put("value", decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.put("value", number.doubleValue());
        } else if (value != null) {
            cell.put("value", value.toString());
        }
        if (colSpan > 1) {
            cell.put("colSpan", colSpan);
        }
        cell.set("style", style);
        return cell;
    }

    private ObjectNode detailStyle(
            ArrayNode templateRows,
            int row,
            int column,
            String background
    ) {
        ObjectNode style = templateStyle(templateRows, row, column);
        style.put("backgroundColor", background);
        style.put("border", "1px solid #333333");
        style.put("verticalAlign", "middle");
        return style;
    }

    private ObjectNode templateStyle(
            ArrayNode rows,
            int rowIndex,
            int columnIndex
    ) {
        ObjectNode row = indexedObject(rows, rowIndex);
        if (row == null || !row.path("cells").isArray()) {
            return defaultStyle();
        }
        ObjectNode cell = indexedObject(
                (ArrayNode) row.path("cells"),
                columnIndex
        );
        if (cell != null && cell.path("style") instanceof ObjectNode style) {
            return style.deepCopy();
        }
        return defaultStyle();
    }

    private ObjectNode defaultStyle() {
        ObjectNode style = objectMapper.createObjectNode();
        style.put("fontFamily", "Noto Sans JP");
        style.put("fontSize", "9pt");
        style.put("border", "1px solid #333333");
        style.put("verticalAlign", "middle");
        return style;
    }

    private ObjectNode indexedObject(ArrayNode values, int expectedIndex) {
        for (int position = 0; position < values.size(); position++) {
            JsonNode value = values.get(position);
            if (!(value instanceof ObjectNode object)) {
                continue;
            }
            int actualIndex = object.path("index").canConvertToInt()
                    ? object.path("index").asInt()
                    : position;
            if (actualIndex == expectedIndex) {
                return object;
            }
        }
        return null;
    }

    private Map<String, List<ReceiptRow>> groupByExpectedMonth(
            List<ReceiptRow> receipts
    ) {
        Map<String, List<ReceiptRow>> groups = new LinkedHashMap<>();
        for (ReceiptRow receipt : receipts) {
            String key = receipt.expectedPaymentDate() == null
                    ? "入金予定日未設定"
                    : "%d年%d月".formatted(
                            receipt.expectedPaymentDate().getYear(),
                            receipt.expectedPaymentDate().getMonthValue()
                    );
            groups.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(receipt);
        }
        return groups;
    }

    private ReceiptRow receiptRow(Map<String, Object> row) {
        return new ReceiptRow(
                longValue(row.get("transaction_id")),
                longValue(row.get("customer_id")),
                text(row.get("customer_name")),
                text(row.get("company_name")),
                text(row.get("closing_rule_text")),
                text(row.get("payment_rule_text")),
                decimal(row.get("billing_amount")),
                date(row.get("expected_payment_date")),
                decimal(row.get("paid_amount")),
                decimal(row.get("fee")),
                decimal(row.get("offset_amount")),
                decimal(row.get("settled_amount")),
                text(row.get("payment_status")),
                text(row.get("note"))
        );
    }

    private String companyName(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> text(row.get("company_name")))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private String statusColor(String status) {
        return switch (status) {
            case "PAID" -> "#C6EFCE";
            case "PARTIAL" -> "#FCE4D6";
            case "OVERPAID" -> "#DDEBF7";
            default -> "#FFF200";
        };
    }

    private String dateText(LocalDate date) {
        return date == null
                ? ""
                : date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    private String sumFormula(int column, List<Integer> rows) {
        if (rows.isEmpty()) {
            return "=0";
        }
        return "=SUM(" + rows.stream()
                .map(row -> columnName(column) + row)
                .collect(java.util.stream.Collectors.joining(","))
                + ")";
    }

    private String columnName(int zeroBasedColumn) {
        int value = zeroBasedColumn + 1;
        StringBuilder result = new StringBuilder();
        while (value > 0) {
            int remainder = (value - 1) % 26;
            result.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return result.toString();
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

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private record ReceiptRow(
            long transactionId,
            long customerId,
            String customerName,
            String companyName,
            String closingRuleText,
            String paymentRuleText,
            BigDecimal billingAmount,
            LocalDate expectedPaymentDate,
            BigDecimal paidAmount,
            BigDecimal fee,
            BigDecimal offsetAmount,
            BigDecimal settledAmount,
            String paymentStatus,
            String note
    ) {
    }
}
