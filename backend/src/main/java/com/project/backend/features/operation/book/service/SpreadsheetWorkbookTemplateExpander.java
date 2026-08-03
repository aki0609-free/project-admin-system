package com.project.backend.features.operation.book.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.entity.ExcelBookVariableMapping;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpreadsheetWorkbookTemplateExpander {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_.]{0,99})}");
    private static final Pattern CELL_REFERENCE =
            Pattern.compile("(\\$?[A-Z]{1,3})(\\$?)(\\d+)");

    private final ObjectMapper objectMapper;

    public JsonNode expand(
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

        ObjectNode result = template.deepCopy();
        ObjectNode workbook = workbookNode(result);
        workbook.put("locale", "ja");

        ArrayNode sheets = requireArray(workbook, "sheets");
        Map<String, ExcelBookVariableMapping> mappings =
                mappingByKey(master.getVariableMappings());
        Map<String, Object> context = buildContext(
                master,
                sourceRows,
                mappings,
                targetMonth,
                generatedAt
        );

        for (JsonNode sheetNode : sheets) {
            if (!(sheetNode instanceof ObjectNode sheet)) {
                throw new IllegalArgumentException(
                        "Spreadsheetのsheet形式が不正です。"
                );
            }
            expandSheet(sheet, sourceRows, mappings, context);
        }

        assertNoUnresolvedPlaceholders(result);
        return result;
    }

    private ObjectNode workbookNode(ObjectNode root) {
        JsonNode wrapped = root.get("Workbook");
        if (wrapped instanceof ObjectNode workbook) {
            return workbook;
        }
        return root;
    }

    private ArrayNode requireArray(ObjectNode parent, String fieldName) {
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

    private Map<String, ExcelBookVariableMapping> mappingByKey(
            List<ExcelBookVariableMapping> mappings
    ) {
        Map<String, ExcelBookVariableMapping> result =
                new LinkedHashMap<>();
        for (ExcelBookVariableMapping mapping : mappings) {
            result.put(mapping.getVariableKey(), mapping);
        }
        return result;
    }

    private Map<String, Object> buildContext(
            ExcelBookMaster master,
            List<Map<String, Object>> sourceRows,
            Map<String, ExcelBookVariableMapping> mappings,
            String targetMonth,
            Instant generatedAt
    ) {
        Map<String, Object> context = new HashMap<>();
        context.put("targetMonth", targetMonth);
        context.put("target_month", targetMonth);
        context.put("bookCode", master.getBookCode());
        context.put("bookName", master.getBookName());
        context.put("generatedAt", generatedAt.toString());

        Map<String, Object> firstRow = sourceRows.isEmpty()
                ? Map.of()
                : sourceRows.getFirst();

        mappings.forEach((key, mapping) -> {
            if ("CONTEXT".equals(mapping.getScope())) {
                context.put(
                        key,
                        convertValue(
                                firstRow.get(mapping.getSourceColumn()),
                                mapping.getDataType()
                        )
                );
            }
        });
        return context;
    }

    private void expandSheet(
            ObjectNode sheet,
            List<Map<String, Object>> sourceRows,
            Map<String, ExcelBookVariableMapping> mappings,
            Map<String, Object> context
    ) {
        ArrayNode rows = sheet.withArray("rows");
        List<Integer> repeatRows = findRepeatRows(rows, mappings);

        if (repeatRows.size() > 1) {
            throw new IllegalArgumentException(
                    "1シートに設定できる明細テンプレート行は1行です。"
            );
        }

        if (repeatRows.isEmpty()) {
            replaceRows(rows, mappings, context, null);
            return;
        }

        int repeatIndex = repeatRows.getFirst();
        ObjectNode templateRow =
                requireObject(rows.get(repeatIndex), "row");
        ArrayNode expandedRows = objectMapper.createArrayNode();

        for (int index = 0; index < repeatIndex; index++) {
            expandedRows.add(rows.get(index).deepCopy());
        }

        List<Map<String, Object>> rowContexts = sourceRows.isEmpty()
                ? List.of(Map.of())
                : sourceRows;

        for (int index = 0; index < rowContexts.size(); index++) {
            ObjectNode expanded = templateRow.deepCopy();
            replaceRow(
                    expanded,
                    mappings,
                    context,
                    rowContexts.get(index)
            );
            shiftFormulas(expanded, index);
            expandedRows.add(expanded);
        }

        for (int index = repeatIndex + 1; index < rows.size(); index++) {
            expandedRows.add(rows.get(index).deepCopy());
        }

        normalizeExplicitRowIndexes(expandedRows);
        sheet.set("rows", expandedRows);
        updateUsedRange(
                sheet,
                Math.max(0, rowContexts.size() - 1)
        );
        replaceRows(expandedRows, mappings, context, null);
    }

    private List<Integer> findRepeatRows(
            ArrayNode rows,
            Map<String, ExcelBookVariableMapping> mappings
    ) {
        List<Integer> result = new ArrayList<>();

        for (int index = 0; index < rows.size(); index++) {
            JsonNode cells = rows.get(index).path("cells");
            if (!cells.isArray()) {
                continue;
            }
            boolean containsRowVariable = false;
            for (JsonNode cell : cells) {
                String value = cell.path("value").isTextual()
                        ? cell.path("value").asText()
                        : "";
                Matcher matcher = PLACEHOLDER.matcher(value);
                while (matcher.find()) {
                    ExcelBookVariableMapping mapping =
                            mappings.get(matcher.group(1));
                    if (mapping != null
                            && "ROW".equals(mapping.getScope())) {
                        containsRowVariable = true;
                        break;
                    }
                }
                if (containsRowVariable) {
                    break;
                }
            }
            if (containsRowVariable) {
                result.add(index);
            }
        }
        return result;
    }

    private void replaceRows(
            ArrayNode rows,
            Map<String, ExcelBookVariableMapping> mappings,
            Map<String, Object> context,
            Map<String, Object> rowContext
    ) {
        for (JsonNode rowNode : rows) {
            if (rowNode instanceof ObjectNode row) {
                replaceRow(row, mappings, context, rowContext);
            }
        }
    }

    private void replaceRow(
            ObjectNode row,
            Map<String, ExcelBookVariableMapping> mappings,
            Map<String, Object> context,
            Map<String, Object> sourceRow
    ) {
        JsonNode cells = row.path("cells");
        if (!cells.isArray()) {
            return;
        }
        for (JsonNode cellNode : cells) {
            if (cellNode instanceof ObjectNode cell) {
                replaceTextField(
                        cell,
                        "value",
                        mappings,
                        context,
                        sourceRow
                );
            }
        }
    }

    private void replaceTextField(
            ObjectNode node,
            String fieldName,
            Map<String, ExcelBookVariableMapping> mappings,
            Map<String, Object> context,
            Map<String, Object> sourceRow
    ) {
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || !valueNode.isTextual()) {
            return;
        }

        String original = valueNode.asText();
        Matcher exact = PLACEHOLDER.matcher(original);
        if (exact.matches()) {
            String key = exact.group(1);
            ResolvedValue resolved = resolve(
                    key,
                    mappings,
                    context,
                    sourceRow
            );
            if (resolved.resolved()) {
                node.set(
                        fieldName,
                        objectMapper.valueToTree(resolved.value())
                );
            }
            return;
        }

        Matcher matcher = PLACEHOLDER.matcher(original);
        StringBuffer replaced = new StringBuffer();
        boolean changed = false;
        while (matcher.find()) {
            ResolvedValue resolved = resolve(
                    matcher.group(1),
                    mappings,
                    context,
                    sourceRow
            );
            if (!resolved.resolved()) {
                continue;
            }
            matcher.appendReplacement(
                    replaced,
                    Matcher.quoteReplacement(
                            resolved.value() == null
                                    ? ""
                                    : String.valueOf(resolved.value())
                    )
            );
            changed = true;
        }
        if (changed) {
            matcher.appendTail(replaced);
            node.put(fieldName, replaced.toString());
        }
    }

    private ResolvedValue resolve(
            String key,
            Map<String, ExcelBookVariableMapping> mappings,
            Map<String, Object> context,
            Map<String, Object> sourceRow
    ) {
        if (context.containsKey(key)) {
            return new ResolvedValue(true, context.get(key));
        }

        ExcelBookVariableMapping mapping = mappings.get(key);
        if (mapping == null) {
            return new ResolvedValue(false, null);
        }

        if (!"ROW".equals(mapping.getScope()) || sourceRow == null) {
            return new ResolvedValue(false, null);
        }

        return new ResolvedValue(
                true,
                convertValue(
                        sourceRow.get(mapping.getSourceColumn()),
                        mapping.getDataType()
                )
        );
    }

    private Object convertValue(Object value, String dataType) {
        if (value == null) {
            return "";
        }

        return switch (dataType) {
            case "NUMBER" -> value instanceof Number
                    ? value
                    : new BigDecimal(String.valueOf(value));
            case "BOOLEAN" -> value instanceof Boolean
                    ? value
                    : Boolean.valueOf(String.valueOf(value));
            case "DATE", "DATETIME", "STRING" ->
                    String.valueOf(value);
            default -> throw new IllegalArgumentException(
                    "未対応の台帳データ型です: " + dataType
            );
        };
    }

    private void shiftFormulas(ObjectNode row, int rowOffset) {
        if (rowOffset == 0) {
            return;
        }
        JsonNode cells = row.path("cells");
        if (!cells.isArray()) {
            return;
        }
        for (JsonNode cellNode : cells) {
            if (!(cellNode instanceof ObjectNode cell)
                    || !cell.path("formula").isTextual()) {
                continue;
            }

            String formula = cell.path("formula").asText();
            Matcher matcher = CELL_REFERENCE.matcher(formula);
            StringBuffer shifted = new StringBuffer();
            while (matcher.find()) {
                if ("$".equals(matcher.group(2))) {
                    matcher.appendReplacement(
                            shifted,
                            Matcher.quoteReplacement(matcher.group())
                    );
                    continue;
                }
                int rowNumber = Integer.parseInt(matcher.group(3));
                String replacement = matcher.group(1)
                        + (rowNumber + rowOffset);
                matcher.appendReplacement(
                        shifted,
                        Matcher.quoteReplacement(replacement)
                );
            }
            matcher.appendTail(shifted);
            cell.put("formula", shifted.toString());
            cell.remove("value");
        }
    }

    private void normalizeExplicitRowIndexes(ArrayNode rows) {
        for (int index = 0; index < rows.size(); index++) {
            JsonNode rowNode = rows.get(index);
            if (rowNode instanceof ObjectNode row
                    && row.has("index")) {
                row.put("index", index);
            }
        }
    }

    private void updateUsedRange(ObjectNode sheet, int addedRows) {
        if (addedRows == 0) {
            return;
        }
        JsonNode usedRangeNode = sheet.get("usedRange");
        if (usedRangeNode instanceof ObjectNode usedRange
                && usedRange.path("rowIndex").canConvertToInt()) {
            usedRange.put(
                    "rowIndex",
                    usedRange.path("rowIndex").asInt() + addedRows
            );
        }
        if (sheet.path("rowCount").canConvertToInt()) {
            sheet.put(
                    "rowCount",
                    sheet.path("rowCount").asInt() + addedRows
            );
        }
    }

    private void assertNoUnresolvedPlaceholders(JsonNode workbook) {
        List<String> unresolved = new ArrayList<>();
        collectUnresolvedPlaceholders(workbook, unresolved);
        if (!unresolved.isEmpty()) {
            throw new IllegalArgumentException(
                    "未登録のテンプレート変数があります: "
                            + String.join(", ", unresolved.stream()
                                    .distinct()
                                    .toList())
            );
        }
    }

    private void collectUnresolvedPlaceholders(
            JsonNode node,
            List<String> unresolved
    ) {
        if (node.isTextual()) {
            Matcher matcher = PLACEHOLDER.matcher(node.asText());
            while (matcher.find()) {
                unresolved.add(matcher.group(1));
            }
            return;
        }
        if (node.isContainerNode()) {
            node.forEach(child -> collectUnresolvedPlaceholders(
                    child,
                    unresolved
            ));
        }
    }

    private ObjectNode requireObject(JsonNode value, String fieldName) {
        if (value instanceof ObjectNode object) {
            return object;
        }
        throw new IllegalArgumentException(
                "Spreadsheetの" + fieldName + "形式が不正です。"
        );
    }

    private record ResolvedValue(
            boolean resolved,
            Object value
    ) {
    }
}
