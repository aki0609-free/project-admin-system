package com.project.backend.features.system.excelbook.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateResponse;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateSaveRequest;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpreadsheetTemplateService {

    private static final String CONTENT_TYPE = "application/json";
    private static final int MAX_TEMPLATE_BYTES = 10 * 1024 * 1024;

    private final ExcelBookMasterRepository repository;
    private final StorageService storageService;
    private final DocumentStorageKeyResolver storageKeyResolver;
    private final ObjectMapper objectMapper;
    private final ExcelBookTemplateRequirementResolver templateRequirementResolver;

    public SpreadsheetTemplateResponse find(Long masterId) {
        ExcelBookMaster master = findMaster(masterId);
        requireTemplateRenderer(master);
        String relativePath = relativePath(master);
        String storageKey = storageKeyResolver.resolve(
                DocumentArea.TEMPLATES,
                relativePath
        );

        if (!storageService.exists(storageKey)) {
            return response(master, relativePath, null);
        }

        try (InputStream inputStream = storageService.load(storageKey)) {
            JsonNode workbook = objectMapper.readTree(inputStream);
            return response(
                    master,
                    relativePath,
                    withReceiptConfirmationScaffold(master, workbook)
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Spreadsheetテンプレートの読み込みに失敗しました。 masterId="
                            + masterId,
                    e
            );
        }
    }

    public SpreadsheetTemplateResponse save(
            Long masterId,
            SpreadsheetTemplateSaveRequest request
    ) {
        ExcelBookMaster master = findMaster(masterId);
        requireTemplateRenderer(master);
        JsonNode workbook = validate(request);
        byte[] data = serialize(workbook);

        String relativePath = relativePath(master);
        String storageKey = storageKeyResolver.resolve(
                DocumentArea.TEMPLATES,
                relativePath
        );

        storageService.save(
                storageKey,
                new ByteArrayInputStream(data),
                data.length,
                CONTENT_TYPE
        );

        return response(master, relativePath, workbook);
    }

    private ExcelBookMaster findMaster(Long masterId) {
        if (masterId == null) {
            throw new IllegalArgumentException("masterId は必須です。");
        }

        return repository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "台帳マスタが見つかりません。id=" + masterId
                ));
    }

    private void requireTemplateRenderer(ExcelBookMaster master) {
        String rendererKey = master.getRendererKey() == null
                ? master.getLayoutType().name()
                : master.getRendererKey();
        if (!templateRequirementResolver.requiresTemplate(rendererKey)) {
            throw new IllegalArgumentException(
                    "コード生成台帳にはSpreadsheetテンプレートを登録できません。"
            );
        }
    }

    private JsonNode validate(SpreadsheetTemplateSaveRequest request) {
        if (request == null
                || request.workbook() == null
                || !request.workbook().isObject()) {
            throw new IllegalArgumentException(
                    "workbook はJSONオブジェクトで指定してください。"
            );
        }

        return request.workbook();
    }

    private byte[] serialize(JsonNode workbook) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(workbook);

            if (data.length > MAX_TEMPLATE_BYTES) {
                throw new IllegalArgumentException(
                        "Spreadsheetテンプレートは10MB以下にしてください。"
                );
            }

            return data;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "SpreadsheetテンプレートのJSON変換に失敗しました。",
                    e
            );
        }
    }

    /**
     * 初期版の入金確認表テンプレートは外観だけを保持し、セル値を持たない。
     * 既存の外観を維持したまま、編集画面で用途が分かる見本値を補完する。
     */
    private JsonNode withReceiptConfirmationScaffold(
            ExcelBookMaster master,
            JsonNode workbook
    ) {
        if (!"RECEIPT_CONFIRMATION".equals(master.getBookCode())
                || hasTemplateValues(workbook)
                || !(workbook instanceof ObjectNode)) {
            return workbook;
        }

        ObjectNode result = workbook.deepCopy();
        ObjectNode workbookNode = result.path("Workbook")
                instanceof ObjectNode nested
                ? nested
                : result;
        if (!(workbookNode.path("sheets") instanceof ArrayNode sheets)
                || sheets.isEmpty()
                || !(sheets.get(0) instanceof ObjectNode sheet)) {
            return workbook;
        }

        ArrayNode rows = sheet.withArray("rows");
        while (rows.size() <= 13) {
            rows.addObject().putArray("cells");
        }

        templateCell(rows, 0, 1, "入金確認表", null, 1);
        templateCell(rows, 0, 10, "会社名（生成時に反映）", null, 1);
        templateCell(rows, 0, 13, "対象年月（生成時に反映）", null, 1);

        String[] headers = {
            "業者名", "締め日", "支払日", "請求金額",
            "入金予定日", "入金額", "手数料", "相殺",
            "その他調整", "合計金額", "備考（調整理由等）"
        };
        int[] headerColumns = {0, 2, 5, 8, 9, 10, 11, 12, 13, 14, 15};
        int[] headerSpans = {2, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1};
        for (int index = 0; index < headers.length; index++) {
            templateCell(
                    rows,
                    2,
                    headerColumns[index],
                    headers[index],
                    "#F2F2F2",
                    headerSpans[index]
            );
        }

        String[] placeholders = {
            "${customerName}", "${closingRule}", "${paymentRule}",
            "${billingAmount}", "${expectedPaymentDate}", "${paidAmount}",
            "${fee}", "${offsetAmount}", "${adjustmentAmount}",
            "${settledAmount}", "${note}"
        };
        for (int index = 0; index < placeholders.length; index++) {
            templateCell(
                    rows,
                    3,
                    headerColumns[index],
                    placeholders[index],
                    "#FFF2CC",
                    headerSpans[index]
            );
        }

        totalScaffoldRow(rows, 9, "対象月 合計", "#D9E1F2");
        totalScaffoldRow(rows, 13, "総合計", "#C9C9F5");
        sheet.putObject("usedRange").put("rowIndex", 13).put("colIndex", 15);
        return result;
    }

    private boolean hasTemplateValues(JsonNode workbook) {
        JsonNode workbookNode = workbook.path("Workbook").isObject()
                ? workbook.path("Workbook")
                : workbook;
        for (JsonNode sheet : workbookNode.path("sheets")) {
            for (JsonNode row : sheet.path("rows")) {
                for (JsonNode cell : row.path("cells")) {
                    if (cell.hasNonNull("value") || cell.hasNonNull("formula")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void totalScaffoldRow(
            ArrayNode rows,
            int rowIndex,
            String label,
            String background
    ) {
        templateCell(rows, rowIndex, 0, label, background, 8);
        for (int column = 8; column <= 15; column++) {
            templateCell(rows, rowIndex, column, "", background, 1);
        }
    }

    private void templateCell(
            ArrayNode rows,
            int rowIndex,
            int columnIndex,
            String value,
            String background,
            int columnSpan
    ) {
        ObjectNode row = (ObjectNode) rows.get(rowIndex);
        ArrayNode cells = row.withArray("cells");
        ObjectNode cell = findCell(cells, columnIndex);
        if (cell == null) {
            cell = cells.addObject();
            cell.put("index", columnIndex);
        }
        cell.put("value", value);
        if (columnSpan > 1) {
            cell.put("colSpan", columnSpan);
        }
        ObjectNode style = cell.path("style") instanceof ObjectNode current
                ? current
                : cell.putObject("style");
        if (!style.has("fontFamily")) {
            style.put("fontFamily", "Noto Sans JP");
        }
        style.put("border", "1px solid #333333");
        style.put("verticalAlign", "middle");
        if (background != null) {
            style.put("backgroundColor", background);
        }
    }

    private ObjectNode findCell(ArrayNode cells, int expectedIndex) {
        for (int position = 0; position < cells.size(); position++) {
            if (!(cells.get(position) instanceof ObjectNode cell)) {
                continue;
            }
            int actualIndex = cell.path("index").canConvertToInt()
                    ? cell.path("index").asInt()
                    : position;
            if (actualIndex == expectedIndex) {
                return cell;
            }
        }
        return null;
    }

    private String relativePath(ExcelBookMaster master) {
        if (master.getTenantId() == null
                || master.getTenantId().isBlank()) {
            throw new IllegalStateException(
                    "台帳マスタのtenantIdが未設定です。id="
                            + master.getId()
            );
        }

        return "ledgers/"
                + master.getTenantId()
                + "/"
                + master.getBookCode()
                + "/template.json";
    }

    private SpreadsheetTemplateResponse response(
            ExcelBookMaster master,
            String relativePath,
            JsonNode workbook
    ) {
        return new SpreadsheetTemplateResponse(
                master.getId(),
                master.getBookCode(),
                relativePath,
                workbook
        );
    }
}
