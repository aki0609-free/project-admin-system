package com.project.backend.features.operation.book.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;

import lombok.RequiredArgsConstructor;

/**
 * 第7期 月間集計表互換のセル配置を担当する。
 *
 * <p>データ取得Viewとセル配置を分離しているため、列追加や計算方法の
 * 変更時はこのクラスまたは新しいレイアウト実装だけを変更できる。</p>
 */
@Component
@RequiredArgsConstructor
public class MonthlySummarySpreadsheetRenderer
        implements SpreadsheetLedgerRenderer {

    public static final String KEY = "MONTHLY_SUMMARY";

    private static final int MAX_GROUPS = 40;
    private static final int FIRST_INPUT_ROW = 6;
    private static final int ROWS_PER_GROUP = 2;
    private static final int FIRST_DAY_COLUMN = 4;
    private static final int COLUMNS_PER_DAY = 4;
    private static final int CUSTOMER_COLUMN = 0;
    private static final int DESCRIPTION_COLUMN = 3;
    private static final int BASE_RATE_COLUMN = 128;
    private static final int OVERTIME_RATE_COLUMN = 129;
    private static final int NIGHT_RATE_COLUMN = 130;
    private static final int YEAR_CELL_ROW = 0;
    private static final int YEAR_CELL_COLUMN = 9;
    private static final int MONTH_CELL_COLUMN = 13;
    private static final int DAILY_PAY_ROW = 90;
    private static final int SOCIAL_INSURANCE_ROW = 89;

    private final ObjectMapper objectMapper;

    @Override
    public String rendererKey() {
        return KEY;
    }

    @Override
    public JsonNode render(SpreadsheetLedgerRenderContext context) {
        return render(
                context.template(),
                context.master(),
                context.sourceRows(),
                context.targetMonth(),
                context.generatedAt()
        );
    }

    @Override
    public boolean editableBeforeClosing() {
        return true;
    }

    @Override
    public boolean usesStableMonthlyPath() {
        return true;
    }

    public JsonNode render(
            JsonNode template,
            ExcelBookMaster master,
            List<Map<String, Object>> sourceRows,
            String targetMonth,
            Instant generatedAt
    ) {
        if (template == null || !template.isObject()) {
            throw new IllegalArgumentException(
                    "Spreadsheetテンプレートが未設定です。"
            );
        }

        YearMonth yearMonth = YearMonth.parse(targetMonth);
        ObjectNode result = template.deepCopy();
        ObjectNode workbook = workbookNode(result);
        workbook.put("locale", "ja");
        ObjectNode sheet = targetSheet(workbook, master);
        sheet.put("name", targetMonth.replace("-", "."));

        ArrayNode rows = requireArray(sheet, "rows");
        setValue(rows, YEAR_CELL_ROW, YEAR_CELL_COLUMN,
                yearMonth.getYear() + "年");
        setValue(rows, YEAR_CELL_ROW, MONTH_CELL_COLUMN,
                yearMonth.getMonthValue() + "月分");

        clearInputArea(rows);
        List<MonthlyGroup> groups = group(sourceRows);
        if (groups.size() > MAX_GROUPS) {
            throw new IllegalArgumentException(
                    "月間集計表の明細上限40行を超えています。件数="
                            + groups.size()
            );
        }

        for (int groupIndex = 0;
                groupIndex < groups.size();
                groupIndex++) {
            writeGroup(rows, groupIndex, groups.get(groupIndex));
        }
        writeDayHeaders(rows, yearMonth);
        writeDailyPay(rows, sourceRows, yearMonth);
        clearCachedFormulaValues(rows);

        ObjectNode metadata = result.withObject(
                "/projectAdminMetadata"
        );
        metadata.put("layoutType", "MONTHLY_SUMMARY");
        metadata.put("bookCode", master.getBookCode());
        metadata.put("targetMonth", targetMonth);
        metadata.put("generatedAt", generatedAt.toString());
        metadata.put("groupCount", groups.size());
        return result;
    }

    /**
     * 再生成時にも画面で手入力した社会保険負担額を維持する。
     */
    @Override
    public void preserveManualInputs(
            JsonNode generated,
            JsonNode existing
    ) {
        ObjectNode generatedSheet = firstSheet(
                workbookNode((ObjectNode) generated)
        );
        ObjectNode existingSheet = firstSheet(
                workbookNode((ObjectNode) existing)
        );
        ArrayNode generatedRows = requireArray(
                generatedSheet,
                "rows"
        );
        ArrayNode existingRows = requireArray(
                existingSheet,
                "rows"
        );
        for (int day = 1; day <= 31; day++) {
            int column = FIRST_DAY_COLUMN
                    + (day - 1) * COLUMNS_PER_DAY;
            ObjectNode existingRow = findIndexedObject(
                    existingRows,
                    SOCIAL_INSURANCE_ROW
            );
            if (existingRow == null
                    || !existingRow.path("cells").isArray()) {
                continue;
            }
            ObjectNode existingCell = findIndexedObject(
                    (ArrayNode) existingRow.path("cells"),
                    column
            );
            if (existingCell == null
                    || !existingCell.has("value")) {
                continue;
            }
            ObjectNode generatedCell = indexedObject(
                    indexedObject(
                            generatedRows,
                            SOCIAL_INSURANCE_ROW,
                            "cells"
                    ).withArray("cells"),
                    column,
                    null
            );
            generatedCell.remove("formula");
            generatedCell.set(
                    "value",
                    existingCell.get("value").deepCopy()
            );
        }
    }

    private List<MonthlyGroup> group(
            List<Map<String, Object>> sourceRows
    ) {
        Map<GroupKey, MonthlyGroup> groups = new LinkedHashMap<>();
        for (Map<String, Object> row : sourceRows) {
            String billingUnit = text(row, "billing_unit");
            if (!"DAILY".equalsIgnoreCase(billingUnit)) {
                throw new IllegalArgumentException(
                        "月間集計表V1は日単価（DAILY）のみ対応です。"
                                + " billingUnit=" + billingUnit
                );
            }

            GroupKey key = new GroupKey(
                    text(row, "customer_name"),
                    text(row, "site_name"),
                    text(row, "job_code"),
                    text(row, "job_name"),
                    text(row, "site_role_code"),
                    text(row, "site_role_name"),
                    decimal(row, "base_unit_price"),
                    decimal(row, "overtime_unit_price"),
                    decimal(row, "night_unit_price")
            );
            MonthlyGroup group = groups.computeIfAbsent(
                    key,
                    MonthlyGroup::new
            );
            LocalDate workDate = date(row, "work_date");
            group.add(
                    workDate.getDayOfMonth(),
                    decimal(row, "person_count"),
                    decimal(row, "overtime_hours"),
                    decimal(row, "night_work_hours"),
                    decimal(row, "other_amount")
            );
        }
        return new ArrayList<>(groups.values());
    }

    private void writeGroup(
            ArrayNode rows,
            int groupIndex,
            MonthlyGroup group
    ) {
        int rowIndex = FIRST_INPUT_ROW
                + groupIndex * ROWS_PER_GROUP;
        GroupKey key = group.key();
        setValue(rows, rowIndex, CUSTOMER_COLUMN,
                key.customerName());
        setValue(rows, rowIndex, DESCRIPTION_COLUMN,
                description(key));
        setValue(rows, rowIndex, BASE_RATE_COLUMN,
                key.baseUnitPrice());
        setValue(rows, rowIndex, OVERTIME_RATE_COLUMN,
                key.overtimeUnitPrice());
        setValue(rows, rowIndex, NIGHT_RATE_COLUMN,
                key.nightUnitPrice());

        group.days().forEach((day, value) -> {
            int column = FIRST_DAY_COLUMN
                    + (day - 1) * COLUMNS_PER_DAY;
            setValue(rows, rowIndex, column,
                    value.personCount());
            setValue(rows, rowIndex, column + 1,
                    value.overtimeHours());
            setValue(rows, rowIndex, column + 2,
                    value.nightWorkHours());
            setValue(rows, rowIndex, column + 3,
                    value.otherAmount());
        });
    }

    private String description(GroupKey key) {
        return java.util.stream.Stream.of(
                        key.siteName(),
                        key.jobName(),
                        key.siteRoleName()
                )
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(" / "));
    }

    private void writeDayHeaders(
            ArrayNode rows,
            YearMonth targetMonth
    ) {
        for (int day = 1; day <= 31; day++) {
            int column = FIRST_DAY_COLUMN
                    + (day - 1) * COLUMNS_PER_DAY;
            if (day <= targetMonth.lengthOfMonth()) {
                setValue(rows, 3, column, day);
            } else {
                clearValue(rows, 3, column);
            }
        }
    }

    private void writeDailyPay(
            ArrayNode rows,
            List<Map<String, Object>> sourceRows,
            YearMonth targetMonth
    ) {
        Map<Integer, BigDecimal> payByDay = new LinkedHashMap<>();
        for (Map<String, Object> row : sourceRows) {
            int day = date(row, "work_date").getDayOfMonth();
            payByDay.merge(
                    day,
                    decimal(row, "estimated_gross_pay_amount"),
                    BigDecimal::add
            );
        }
        for (int day = 1; day <= targetMonth.lengthOfMonth(); day++) {
            int column = FIRST_DAY_COLUMN
                    + (day - 1) * COLUMNS_PER_DAY;
            setValue(
                    rows,
                    DAILY_PAY_ROW,
                    column,
                    payByDay.getOrDefault(day, BigDecimal.ZERO)
            );
        }
    }

    private void clearInputArea(ArrayNode rows) {
        for (int group = 0; group < MAX_GROUPS; group++) {
            int row = FIRST_INPUT_ROW + group * ROWS_PER_GROUP;
            clearValue(rows, row, CUSTOMER_COLUMN);
            clearValue(rows, row, DESCRIPTION_COLUMN);
            clearValue(rows, row, BASE_RATE_COLUMN);
            clearValue(rows, row, OVERTIME_RATE_COLUMN);
            clearValue(rows, row, NIGHT_RATE_COLUMN);
            for (int day = 1; day <= 31; day++) {
                int column = FIRST_DAY_COLUMN
                        + (day - 1) * COLUMNS_PER_DAY;
                for (int offset = 0; offset < 4; offset++) {
                    clearValue(rows, row, column + offset);
                }
            }
        }
        for (int day = 1; day <= 31; day++) {
            clearValue(
                    rows,
                    DAILY_PAY_ROW,
                    FIRST_DAY_COLUMN
                            + (day - 1) * COLUMNS_PER_DAY
            );
        }
    }

    private void clearCachedFormulaValues(ArrayNode rows) {
        for (JsonNode rowNode : rows) {
            JsonNode cells = rowNode.path("cells");
            if (!cells.isArray()) {
                continue;
            }
            for (JsonNode cellNode : cells) {
                if (cellNode instanceof ObjectNode cell
                        && cell.path("formula").isTextual()) {
                    cell.remove("value");
                }
            }
        }
    }

    private void setValue(
            ArrayNode rows,
            int rowIndex,
            int columnIndex,
            Object value
    ) {
        ObjectNode cell = indexedObject(
                indexedObject(rows, rowIndex, "cells")
                        .withArray("cells"),
                columnIndex,
                null
        );
        cell.remove("formula");
        cell.set("value", objectMapper.valueToTree(value));
    }

    private void clearValue(
            ArrayNode rows,
            int rowIndex,
            int columnIndex
    ) {
        ObjectNode row = findIndexedObject(rows, rowIndex);
        if (row == null || !row.path("cells").isArray()) {
            return;
        }
        ObjectNode cell = findIndexedObject(
                (ArrayNode) row.path("cells"),
                columnIndex
        );
        if (cell != null && !cell.has("formula")) {
            cell.remove("value");
        }
    }

    private ObjectNode indexedObject(
            ArrayNode array,
            int index,
            String childArrayName
    ) {
        ObjectNode existing = findIndexedObject(array, index);
        if (existing != null) {
            if (childArrayName != null) {
                existing.withArray(childArrayName);
            }
            return existing;
        }
        ObjectNode created = objectMapper.createObjectNode();
        created.put("index", index);
        if (childArrayName != null) {
            created.putArray(childArrayName);
        }
        array.add(created);
        return created;
    }

    private ObjectNode findIndexedObject(
            ArrayNode array,
            int index
    ) {
        for (int position = 0; position < array.size(); position++) {
            JsonNode node = array.get(position);
            if (!(node instanceof ObjectNode object)) {
                continue;
            }
            int actualIndex = object.path("index").canConvertToInt()
                    ? object.path("index").asInt()
                    : position;
            if (actualIndex == index) {
                return object;
            }
        }
        return null;
    }

    private ObjectNode workbookNode(ObjectNode root) {
        JsonNode wrapped = root.get("Workbook");
        return wrapped instanceof ObjectNode workbook
                ? workbook
                : root;
    }

    private ObjectNode targetSheet(
            ObjectNode workbook,
            ExcelBookMaster master
    ) {
        ArrayNode sheets = requireArray(workbook, "sheets");
        for (JsonNode sheetNode : sheets) {
            if (sheetNode instanceof ObjectNode sheet
                    && master.getTemplateSheetName().equals(
                            sheet.path("name").asText()
                    )) {
                return sheet;
            }
        }
        if (!sheets.isEmpty()
                && sheets.get(0) instanceof ObjectNode sheet) {
            return sheet;
        }
        throw new IllegalArgumentException(
                "Spreadsheetテンプレートにシートがありません。"
        );
    }

    private ObjectNode firstSheet(ObjectNode workbook) {
        ArrayNode sheets = requireArray(workbook, "sheets");
        if (!sheets.isEmpty()
                && sheets.get(0) instanceof ObjectNode sheet) {
            return sheet;
        }
        throw new IllegalArgumentException(
                "Spreadsheetテンプレートにシートがありません。"
        );
    }

    private ArrayNode requireArray(
            ObjectNode parent,
            String fieldName
    ) {
        JsonNode value = parent.get(fieldName);
        if (value instanceof ArrayNode array) {
            return array;
        }
        throw new IllegalArgumentException(
                "Spreadsheetテンプレートに"
                        + fieldName
                        + "がありません。"
        );
    }

    private String text(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private BigDecimal decimal(
            Map<String, Object> row,
            String key
    ) {
        Object value = value(row, key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal
                ? decimal
                : new BigDecimal(String.valueOf(value));
    }

    private LocalDate date(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value != null) {
            return LocalDate.parse(String.valueOf(value));
        }
        throw new IllegalArgumentException(
                "月間集計データに" + key + "がありません。"
        );
    }

    private Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return row.entrySet().stream()
                .filter(entry ->
                        entry.getKey().toLowerCase(Locale.ROOT)
                                .equals(normalized)
                )
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private record GroupKey(
            String customerName,
            String siteName,
            String jobCode,
            String jobName,
            String siteRoleCode,
            String siteRoleName,
            BigDecimal baseUnitPrice,
            BigDecimal overtimeUnitPrice,
            BigDecimal nightUnitPrice
    ) {
    }

    private record DayValue(
            BigDecimal personCount,
            BigDecimal overtimeHours,
            BigDecimal nightWorkHours,
            BigDecimal otherAmount
    ) {
        private DayValue add(
                BigDecimal person,
                BigDecimal overtime,
                BigDecimal night,
                BigDecimal other
        ) {
            return new DayValue(
                    personCount.add(person),
                    overtimeHours.add(overtime),
                    nightWorkHours.add(night),
                    otherAmount.add(other)
            );
        }
    }

    private static final class MonthlyGroup {
        private final GroupKey key;
        private final Map<Integer, DayValue> days =
                new LinkedHashMap<>();

        private MonthlyGroup(GroupKey key) {
            this.key = key;
        }

        private void add(
                int day,
                BigDecimal person,
                BigDecimal overtime,
                BigDecimal night,
                BigDecimal other
        ) {
            days.compute(
                    day,
                    (ignored, current) -> current == null
                            ? new DayValue(
                                    person,
                                    overtime,
                                    night,
                                    other
                            )
                            : current.add(
                                    person,
                                    overtime,
                                    night,
                                    other
                            )
            );
        }

        private GroupKey key() {
            return key;
        }

        private Map<Integer, DayValue> days() {
            return days;
        }
    }
}
