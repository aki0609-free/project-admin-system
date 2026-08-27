package com.project.backend.features.operation.book.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;
import com.project.backend.features.operation.book.dto.OperationExcelBookResponse;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerGenerationMode;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerGenerateResponse;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.dto.ExcelBookPrintConfig;
import com.project.backend.features.system.excelbook.dto.ExcelBookSelectionConfig;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;
import com.project.backend.features.system.excelbook.service.ExcelBookDataSourceCatalogService;
import com.project.backend.features.system.excelbook.service.SpreadsheetTemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpreadsheetLedgerGenerationService {

    private static final Pattern SAFE_SEGMENT =
            Pattern.compile("[A-Za-z0-9_-]+");
    private static final int MAX_GENERATED_BYTES = 20 * 1024 * 1024;
    private static final String CONTENT_TYPE = "application/json";
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("uuuuMMdd-HHmmssSSS");

    private final ExcelBookMasterRepository repository;
    private final ExcelBookDataSourceCatalogService catalogService;
    private final SpreadsheetTemplateService templateService;
    private final ExcelBookDataSourceRowQueryService rowQueryService;
    private final SpreadsheetLedgerRendererRegistry rendererRegistry;
    private final SpreadsheetLedgerSelectionService selectionService;
    private final MonthlyClosingRepository closingRepository;
    private final StorageService storageService;
    private final DocumentStorageKeyResolver storageKeyResolver;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public List<OperationExcelBookResponse> findActive() {
        return repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByBookNameAsc()
                .stream()
                .map(master -> {
                    SpreadsheetLedgerRenderer renderer = renderer(master);
                    boolean usesTemplate = renderer.requiresTemplate();
                    boolean templateConfigured = usesTemplate
                            && templateService.find(master.getId())
                                    .workbook() != null;
                    return new OperationExcelBookResponse(
                            master.getId(),
                            master.getBookCode(),
                            master.getBookName(),
                            master.getSourceName(),
                            usesTemplate
                                    ? SpreadsheetLedgerGenerationMode.TEMPLATE
                                    : SpreadsheetLedgerGenerationMode.CODE,
                            !usesTemplate || templateConfigured,
                            templateConfigured,
                            new ExcelBookSelectionConfig(
                                    master.getSelectionMode(),
                                    master.getSelectionSourceName(),
                                    master.getSelectionValueColumn(),
                                    splitColumns(
                                            master.getSelectionDisplayColumns()
                                    ),
                                    master.getAllowSelectAll(),
                                    master.getGenerationUnit()
                            ),
                            new ExcelBookPrintConfig(
                                    master.getPrintPaperSize(),
                                    master.getPrintOrientation(),
                                    master.getPrintFitToOnePage()
                            )
                    );
                })
                .toList();
    }

    public SpreadsheetLedgerGenerateResponse generate(
            String bookCode,
            String targetMonth
    ) {
        validateBookCode(bookCode);
        YearMonth.parse(targetMonth);
        ExcelBookMaster master = findMaster(bookCode);
        if (master.getSelectionMode()
                != com.project.backend.features.system.excelbook.enums
                .ExcelBookSelectionMode.NONE) {
            throw new IllegalArgumentException(
                    "この台帳は対象を選択して生成してください。"
            );
        }
        return generateOne(master, targetMonth, null, null);
    }

    public List<SpreadsheetLedgerGenerateResponse> generateSelected(
            String bookCode,
            String targetMonth,
            List<String> selectionValues
    ) {
        validateBookCode(bookCode);
        YearMonth.parse(targetMonth);
        ExcelBookMaster master = findMaster(bookCode);
        if (master.getSelectionMode()
                == com.project.backend.features.system.excelbook.enums
                .ExcelBookSelectionMode.NONE) {
            throw new IllegalArgumentException(
                    "この台帳には対象選択が設定されていません。"
            );
        }
        LinkedHashSet<String> requested = selectionValues == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(selectionValues);
        requested.removeIf(value ->
                !StringUtils.hasText(value)
        );
        if (requested.isEmpty()) {
            throw new IllegalArgumentException(
                    "生成対象を1件以上選択してください。"
            );
        }

        var options = selectionService.find(bookCode, targetMonth);
        var allowed = options.options().stream()
                .map(option -> option.value())
                .collect(java.util.stream.Collectors.toSet());
        if (!allowed.containsAll(requested)) {
            throw new IllegalArgumentException(
                    "選択一覧に存在しない生成対象が含まれています。"
            );
        }
        return requested.stream()
                .map(value -> generateOne(
                        master,
                        targetMonth,
                        value,
                        null
                ))
                .toList();
    }

    /**
     * 月次締めVersionの確定台帳を生成する。
     * 対象選択型は、対象月に存在する全選択肢を個別ファイル化する。
     */
    public List<SpreadsheetLedgerGenerateResponse> generateForClosing(
            String bookCode,
            String targetMonth,
            Integer closingVersion
    ) {
        validateBookCode(bookCode);
        YearMonth.parse(targetMonth);
        if (closingVersion == null || closingVersion < 1) {
            throw new IllegalArgumentException(
                    "closingVersionは1以上で指定してください。"
            );
        }

        ExcelBookMaster master = findMaster(bookCode);
        if (master.getSelectionMode()
                == com.project.backend.features.system.excelbook.enums
                .ExcelBookSelectionMode.NONE) {
            return List.of(generateOne(
                    master,
                    targetMonth,
                    null,
                    closingVersion
            ));
        }

        List<String> selectionValues = selectionService
                .find(bookCode, targetMonth)
                .options()
                .stream()
                .map(option -> option.value())
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (selectionValues.isEmpty()) {
            throw new IllegalStateException(
                    "月次締め台帳の生成対象がありません。bookCode="
                            + bookCode
            );
        }
        return selectionValues.stream()
                .map(value -> generateOne(
                        master,
                        targetMonth,
                        value,
                        closingVersion
                ))
                .toList();
    }

    private SpreadsheetLedgerGenerateResponse generateOne(
            ExcelBookMaster master,
            String targetMonth,
            String selectionValue,
            Integer closingVersion
    ) {
        long startedAtNanos = System.nanoTime();

        if (master.getSourceType() != ExcelBookSourceType.SNAPSHOT) {
            throw new UnsupportedOperationException(
                    "V1の台帳生成はSNAPSHOTだけに対応しています。"
            );
        }
        SpreadsheetLedgerRenderer renderer = renderer(master);
        boolean editable = isEditable(
                renderer,
                targetMonth
        );
        if (closingVersion == null
                && renderer.editableBeforeClosing()
                && !editable) {
            throw new IllegalStateException(
                    targetMonth + " は締め済みのため再生成できません。"
            );
        }
        if (renderer.requiresVariableMappings()
                && master.getVariableMappings().isEmpty()) {
            throw new IllegalArgumentException(
                    "台帳テンプレート変数が設定されていません。"
            );
        }

        var catalog = catalogService.findRequired(
                master.getSourceName()
        );
        var rows = renderer.usesAllSourceColumns()
                ? findSourceRows(
                        master,
                        catalog,
                        targetMonth,
                        selectionValue
                )
                : rowQueryService.findRows(
                        catalog,
                        master.getVariableMappings(),
                        targetMonth
                );
        JsonNode templateWorkbook = objectMapper.createObjectNode();
        if (renderer.requiresTemplate()) {
            var template = templateService.find(master.getId());
            if (template.workbook() == null) {
                throw new IllegalArgumentException(
                        "Spreadsheetテンプレートが保存されていません。"
                );
            }
            templateWorkbook = template.workbook();
        }

        Instant generatedAt = Instant.now(clock);
        JsonNode workbook = renderer.render(
                new SpreadsheetLedgerRenderContext(
                        templateWorkbook,
                        master,
                        rows,
                        targetMonth,
                        generatedAt,
                        selectionValue == null
                                ? Map.of()
                                : Map.of(
                                        "selectionValue",
                                        selectionValue
                                )
                )
        );
        addClosingMetadata(workbook, closingVersion);
        String relativePath = buildRelativePath(
                master,
                targetMonth,
                generatedAt,
                selectionValue,
                closingVersion
        );
        String storageKey = storageKeyResolver.resolve(
                DocumentArea.GENERATED_REPORTS,
                relativePath
        );
        preserveManualInputs(
                renderer,
                master,
                targetMonth,
                selectionValue,
                generatedAt,
                closingVersion,
                storageKey,
                workbook
        );
        byte[] data = serialize(workbook);

        storageService.save(
                storageKey,
                new ByteArrayInputStream(data),
                data.length,
                CONTENT_TYPE
        );

        return new SpreadsheetLedgerGenerateResponse(
                master.getId(),
                master.getBookCode(),
                master.getBookName(),
                targetMonth,
                rows.size(),
                generatedAt,
                relativePath,
                data.length,
                Math.max(
                        0L,
                        (System.nanoTime() - startedAtNanos)
                                / 1_000_000L
                ),
                closingVersion == null && editable,
                workbook,
                selectionValue
        );
    }

    private List<Map<String, Object>> findSourceRows(
            ExcelBookMaster master,
            com.project.backend.features.system.excelbook.entity
                    .ExcelBookDataSourceCatalog catalog,
            String targetMonth,
            String selectionValue
    ) {
        if (selectionValue == null) {
            return rowQueryService.findAllRows(catalog, targetMonth);
        }
        return rowQueryService.findAllRows(
                catalog,
                targetMonth,
                master.getSelectionValueColumn(),
                List.of(selectionValue)
        );
    }

    private void preserveManualInputs(
            SpreadsheetLedgerRenderer renderer,
            ExcelBookMaster master,
            String targetMonth,
            String selectionValue,
            Instant generatedAt,
            Integer closingVersion,
            String storageKey,
            JsonNode generated
    ) {
        if (!renderer.usesStableMonthlyPath()) {
            return;
        }
        String existingStorageKey = storageKey;
        if (closingVersion != null) {
            String workingRelativePath = buildRelativePath(
                    master,
                    targetMonth,
                    generatedAt,
                    selectionValue,
                    null
            );
            existingStorageKey = storageKeyResolver.resolve(
                    DocumentArea.GENERATED_REPORTS,
                    workingRelativePath
            );
        }
        if (!storageService.exists(existingStorageKey)) {
            return;
        }
        try (InputStream input = storageService.load(existingStorageKey)) {
            JsonNode existing = objectMapper.readTree(input);
            renderer.preserveManualInputs(
                    generated,
                    existing
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "既存月間集計表の手入力値を復元できません。",
                    e
            );
        }
    }

    private boolean isEditable(
            SpreadsheetLedgerRenderer renderer,
            String targetMonth
    ) {
        if (!renderer.editableBeforeClosing()) {
            return false;
        }
        if (renderer.editableAfterMonthlyClosing()) {
            return true;
        }
        return closingRepository
                .findByTargetMonthAndDeletedAtIsNull(
                        YearMonth.parse(targetMonth).atDay(1)
                )
                .map(entity ->
                        entity.getStatus()
                                != MonthlyClosingStatus.CLOSED
                )
                .orElse(true);
    }

    private byte[] serialize(JsonNode workbook) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(workbook);
            if (data.length > MAX_GENERATED_BYTES) {
                throw new IllegalArgumentException(
                        "生成台帳は20MB以下にしてください。"
                );
            }
            return data;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "生成台帳のJSON変換に失敗しました。",
                    e
            );
        }
    }

    private String buildRelativePath(
            ExcelBookMaster master,
            String targetMonth,
            Instant generatedAt,
            String selectionValue,
            Integer closingVersion
    ) {
        String tenantId = master.getTenantId();
        if (!StringUtils.hasText(tenantId)
                || !SAFE_SEGMENT.matcher(tenantId).matches()) {
            throw new IllegalStateException(
                    "台帳マスタのtenantIdが不正です。"
            );
        }

        String directory = "ledgers/"
                + tenantId
                + "/"
                + master.getBookCode()
                + "/"
                + targetMonth
                + "/";
        if (closingVersion != null) {
            directory += "closing/v" + closingVersion + "/";
        }
        if (selectionValue != null) {
            if (!SAFE_SEGMENT.matcher(selectionValue).matches()) {
                throw new IllegalArgumentException(
                        "選択値をS3キーへ使用できません: "
                                + selectionValue
                );
            }
            directory += "selections/" + selectionValue + "/";
        }
        if (closingVersion != null) {
            return directory
                    + master.getBookCode()
                    + "-"
                    + targetMonth
                    + (selectionValue == null
                            ? ""
                            : "-" + selectionValue)
                    + ".json";
        }
        if (renderer(master).usesStableMonthlyPath()) {
            return directory
                    + master.getBookCode()
                    + "-"
                    + targetMonth
                    + ".json";
        }
        return directory
                + master.getBookCode()
                + "-"
                + targetMonth
                + "-"
                + FILE_TIMESTAMP
                .withZone(clock.getZone())
                .format(generatedAt)
                + ".json";
    }

    private void addClosingMetadata(
            JsonNode workbook,
            Integer closingVersion
    ) {
        if (closingVersion == null
                || !(workbook instanceof com.fasterxml.jackson.databind.node
                .ObjectNode root)) {
            return;
        }
        root.withObject("/projectAdminMetadata")
                .put("closingVersion", closingVersion)
                .put("finalized", true);
    }

    private SpreadsheetLedgerRenderer renderer(
            ExcelBookMaster master
    ) {
        String rendererKey = StringUtils.hasText(master.getRendererKey())
                ? master.getRendererKey()
                : master.getLayoutType().name();
        return rendererRegistry.findRequired(rendererKey);
    }

    private ExcelBookMaster findMaster(String bookCode) {
        return repository
                .findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        bookCode
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "有効な台帳マスタが見つかりません: " + bookCode
                ));
    }

    private List<String> splitColumns(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(column -> !column.isEmpty())
                .toList();
    }

    private void validateBookCode(String bookCode) {
        if (!StringUtils.hasText(bookCode)
                || !SAFE_SEGMENT.matcher(bookCode).matches()) {
            throw new IllegalArgumentException(
                    "bookCodeが不正です。"
            );
        }
    }
}
