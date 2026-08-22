package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;

class ReceiptConfirmationSpreadsheetRendererTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReceiptConfirmationSpreadsheetRenderer renderer =
            new ReceiptConfirmationSpreadsheetRenderer(objectMapper);

    @Test
    void render_shouldGroupByExpectedMonthAndKeepEditableIds() {
        var result = renderer.render(new SpreadsheetLedgerRenderContext(
                template(),
                master(),
                List.of(
                        row(1, 10, "顧客A", "2026-03-31", "1133464", "550", "0", "PAID"),
                        row(2, 20, "顧客B", "2026-04-10", "0", "0", "0", "UNPAID")
                ),
                "2026-02",
                Instant.parse("2026-02-28T00:00:00Z"),
                Map.of()
        ));

        var sheet = result.path("Workbook").path("sheets").get(0);
        var rows = (ArrayNode) sheet.path("rows");

        assertThat(rows).hasSize(8);
        assertThat(cell(rows.get(3), 14).path("formula").asText())
                .isEqualTo("=SUM(K4:N4)");
        assertThat(cell(rows.get(3), 16).path("value").asDouble())
                .isEqualTo(1d);
        assertThat(cell(rows.get(3), 17).path("value").asDouble())
                .isEqualTo(10d);
        assertThat(cell(rows.get(4), 0).path("value").asText())
                .isEqualTo("2026年3月 合計");
        assertThat(cell(rows.get(6), 0).path("value").asText())
                .isEqualTo("2026年4月 合計");
        assertThat(cell(rows.get(7), 0).path("value").asText())
                .isEqualTo("総合計");
        assertThat(renderer.editableAfterMonthlyClosing()).isTrue();
        assertThat(sheet.path("isProtected").asBoolean()).isTrue();
        assertThat(cell(rows.get(3), 10).path("isLocked").asBoolean())
                .isFalse();
        assertThat(cell(rows.get(3), 8).path("isLocked").asBoolean())
                .isTrue();
        assertThat(result.path("projectAdminMetadata")
                .path("transactionCount").asInt()).isEqualTo(2);
    }

    private JsonNode template() {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode workbook = root.putObject("Workbook");
        ObjectNode sheet = workbook.putArray("sheets").addObject();
        sheet.put("name", "Sheet1");
        ArrayNode rows = sheet.putArray("rows");
        for (int index = 0; index < 14; index++) {
            rows.addObject().putArray("cells");
        }
        return root;
    }

    private ExcelBookMaster master() {
        ExcelBookMaster master = new ExcelBookMaster();
        master.setBookCode("RECEIPT_CONFIRMATION");
        return master;
    }

    private Map<String, Object> row(
            long transactionId,
            long customerId,
            String customerName,
            String expectedDate,
            String paid,
            String fee,
            String offset,
            String status
    ) {
        BigDecimal paidAmount = new BigDecimal(paid);
        BigDecimal feeAmount = new BigDecimal(fee);
        BigDecimal offsetAmount = new BigDecimal(offset);
        return Map.ofEntries(
                Map.entry("transaction_id", transactionId),
                Map.entry("customer_id", customerId),
                Map.entry("customer_name", customerName),
                Map.entry("company_name", "株式会社富陽"),
                Map.entry("closing_rule_text", "毎月 末日"),
                Map.entry("payment_rule_text", "翌々 10日"),
                Map.entry("billing_amount", new BigDecimal("1134014")),
                Map.entry("expected_payment_date", LocalDate.parse(expectedDate)),
                Map.entry("paid_amount", paidAmount),
                Map.entry("fee", feeAmount),
                Map.entry("offset_amount", offsetAmount),
                Map.entry("adjustment_amount", BigDecimal.ZERO),
                Map.entry("settled_amount", paidAmount.add(feeAmount).add(offsetAmount)),
                Map.entry("payment_status", status),
                Map.entry("note", "")
        );
    }

    private JsonNode cell(JsonNode row, int expectedIndex) {
        ArrayNode cells = (ArrayNode) row.path("cells");
        for (int position = 0; position < cells.size(); position++) {
            JsonNode cell = cells.get(position);
            int actualIndex = cell.path("index").canConvertToInt()
                    ? cell.path("index").asInt()
                    : position;
            if (actualIndex == expectedIndex) {
                return cell;
            }
        }
        return objectMapper.createObjectNode();
    }
}
