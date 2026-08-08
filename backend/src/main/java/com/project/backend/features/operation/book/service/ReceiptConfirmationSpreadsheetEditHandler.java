package com.project.backend.features.operation.book.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.backend.features.customer.dto.CustomerPaymentConfirmRequest;
import com.project.backend.features.customer.entity.CustomerTransaction;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;
import com.project.backend.features.customer.service.CustomerTransactionCommandService;

import lombok.RequiredArgsConstructor;

/**
 * 入金確認表の編集可能セルをcustomer_transactionsへ反映する。
 */
@Component
@RequiredArgsConstructor
public class ReceiptConfirmationSpreadsheetEditHandler
        implements SpreadsheetLedgerEditHandler {

    private static final int MAX_NOTE_LENGTH = 255;

    private final CustomerTransactionRepository repository;
    private final CustomerTransactionCommandService commandService;
    private final Clock clock;

    @Override
    public String rendererKey() {
        return ReceiptConfirmationSpreadsheetRenderer.KEY;
    }

    @Override
    @Transactional
    public void apply(String targetMonth, JsonNode workbook) {
        ArrayNode rows = rows(workbook);
        Set<Long> processed = new HashSet<>();
        for (JsonNode value : rows) {
            if (!(value instanceof ObjectNode row)
                    || !row.path("cells").isArray()) {
                continue;
            }
            ArrayNode cells = (ArrayNode) row.path("cells");
            Long transactionId = optionalLong(
                    cellValue(
                            cells,
                            ReceiptConfirmationSpreadsheetRenderer
                                    .TRANSACTION_ID_COLUMN
                    )
            );
            Long customerId = optionalLong(
                    cellValue(
                            cells,
                            ReceiptConfirmationSpreadsheetRenderer
                                    .CUSTOMER_ID_COLUMN
                    )
            );
            if (transactionId == null && customerId == null) {
                continue;
            }
            if (transactionId == null || customerId == null) {
                throw new IllegalArgumentException(
                        "入金確認表の非表示ID列が不正です。"
                );
            }
            if (!processed.add(transactionId)) {
                throw new IllegalArgumentException(
                        "入金確認表に同じ取引が重複しています。id="
                                + transactionId
                );
            }

            CustomerTransaction transaction = repository
                    .findByIdAndDeletedAtIsNull(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "入金取引が見つかりません。id=" + transactionId
                    ));
            if (!customerId.equals(transaction.getCustomerId())
                    || !targetMonth.equals(transaction.getTargetMonth())) {
                throw new IllegalArgumentException(
                        "入金確認表の取引情報が一致しません。id="
                                + transactionId
                );
            }

            int billingAmount = yen(
                    cellValue(
                            cells,
                            ReceiptConfirmationSpreadsheetRenderer
                                    .BILLING_AMOUNT_COLUMN
                    ),
                    "請求金額"
            );
            int persistedBillingAmount = transaction.getBillingAmount() == null
                    ? 0
                    : transaction.getBillingAmount();
            if (billingAmount != persistedBillingAmount) {
                throw new IllegalArgumentException(
                        "請求金額は入金確認表から変更できません。取引ID="
                                + transactionId
                );
            }

            int paidAmount = yen(
                    cellValue(
                            cells,
                            ReceiptConfirmationSpreadsheetRenderer
                                    .PAID_AMOUNT_COLUMN
                    ),
                    "入金額"
            );
            int fee = yen(
                    cellValue(
                            cells,
                            ReceiptConfirmationSpreadsheetRenderer.FEE_COLUMN
                    ),
                    "手数料"
            );
            int offsetAmount = yen(
                    cellValue(
                            cells,
                            ReceiptConfirmationSpreadsheetRenderer.OFFSET_COLUMN
                    ),
                    "相殺"
            );
            String note = text(
                    cellValue(
                            cells,
                            ReceiptConfirmationSpreadsheetRenderer.NOTE_COLUMN
                    )
            );
            if (note.length() > MAX_NOTE_LENGTH) {
                throw new IllegalArgumentException(
                        "備考は255文字以内で入力してください。取引ID="
                                + transactionId
                );
            }

            int settledAmount;
            try {
                settledAmount = Math.addExact(
                        Math.addExact(paidAmount, fee),
                        offsetAmount
                );
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "入金額・手数料・相殺の合計が上限を超えています。取引ID="
                                + transactionId,
                        exception
                );
            }
            LocalDate confirmedPaymentDate = settledAmount > 0
                    ? transaction.getConfirmedPaymentDate() != null
                            ? transaction.getConfirmedPaymentDate()
                            : LocalDate.now(clock)
                    : null;

            commandService.confirmPaymentFromLedger(
                    customerId,
                    transactionId,
                    targetMonth,
                    new CustomerPaymentConfirmRequest(
                            confirmedPaymentDate,
                            paidAmount,
                            fee,
                            offsetAmount,
                            note
                    )
            );
        }
    }

    private ArrayNode rows(JsonNode workbook) {
        JsonNode root = workbook.path("Workbook").isObject()
                ? workbook.path("Workbook")
                : workbook;
        JsonNode sheets = root.path("sheets");
        if (!sheets.isArray() || sheets.isEmpty()) {
            throw new IllegalArgumentException(
                    "入金確認表のシートが見つかりません。"
            );
        }
        JsonNode rows = sheets.get(0).path("rows");
        if (!(rows instanceof ArrayNode array)) {
            throw new IllegalArgumentException(
                    "入金確認表の行データが見つかりません。"
            );
        }
        return array;
    }

    private JsonNode cellValue(ArrayNode cells, int expectedIndex) {
        for (int position = 0; position < cells.size(); position++) {
            JsonNode value = cells.get(position);
            if (!value.isObject()) {
                continue;
            }
            int actualIndex = value.path("index").canConvertToInt()
                    ? value.path("index").asInt()
                    : position;
            if (actualIndex == expectedIndex) {
                return value.get("value");
            }
        }
        return null;
    }

    private int yen(JsonNode value, String fieldName) {
        if (value == null || value.isNull()
                || value.asText().isBlank()) {
            return 0;
        }
        String normalized = value.isNumber()
                ? value.asText()
                : value.asText()
                        .replace(",", "")
                        .replace("¥", "")
                        .trim();
        try {
            BigDecimal decimal = new BigDecimal(normalized);
            if (decimal.signum() < 0
                    || decimal.stripTrailingZeros().scale() > 0) {
                throw new NumberFormatException();
            }
            return decimal.intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(
                    fieldName + "は0以上の円単位整数で入力してください。value="
                            + value.asText(),
                    exception
            );
        }
    }

    private Long optionalLong(JsonNode value) {
        if (value == null || value.isNull()
                || value.asText().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText()).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "非表示ID列が数値ではありません。",
                    exception
            );
        }
    }

    private String text(JsonNode value) {
        return value == null || value.isNull()
                ? ""
                : value.asText().trim();
    }
}
