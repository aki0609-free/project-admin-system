package com.project.backend.features.operation.book.service;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
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
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerSaveResponse;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SpreadsheetLedgerEditingService {

    private static final Pattern SAFE_SEGMENT =
            Pattern.compile("[A-Za-z0-9_-]+");
    private static final int MAX_WORKBOOK_BYTES = 20 * 1024 * 1024;
    private static final String CONTENT_TYPE = "application/json";

    private final ExcelBookMasterRepository masterRepository;
    private final SpreadsheetLedgerRendererRegistry rendererRegistry;
    private final SpreadsheetLedgerEditHandlerRegistry editHandlerRegistry;
    private final MonthlyClosingRepository closingRepository;
    private final StorageService storageService;
    private final DocumentStorageKeyResolver storageKeyResolver;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SpreadsheetLedgerSaveResponse save(
            String bookCode,
            String targetMonth,
            JsonNode workbook
    ) {
        return save(bookCode, targetMonth, null, workbook);
    }

    public SpreadsheetLedgerSaveResponse save(
            String bookCode,
            String targetMonth,
            String selectionValue,
            JsonNode workbook
    ) {
        validateBookCode(bookCode);
        YearMonth month = YearMonth.parse(targetMonth);
        if (workbook == null || !workbook.isObject()) {
            throw new IllegalArgumentException(
                    "SpreadsheetのWorkbook JSONは必須です。"
            );
        }

        ExcelBookMaster master = masterRepository
                .findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        bookCode
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "有効な台帳マスタが見つかりません: " + bookCode
                ));
        if (master.getSelectionMode()
                == com.project.backend.features.system.excelbook.enums
                .ExcelBookSelectionMode.NONE
                && selectionValue != null) {
            throw new IllegalArgumentException(
                    "この台帳には選択値を指定できません。"
            );
        }
        if (master.getSelectionMode()
                != com.project.backend.features.system.excelbook.enums
                .ExcelBookSelectionMode.NONE) {
            if (!StringUtils.hasText(selectionValue)
                    || !SAFE_SEGMENT.matcher(selectionValue).matches()) {
                throw new IllegalArgumentException(
                        "選択値が不正です。"
                );
            }
        }
        SpreadsheetLedgerRenderer renderer = renderer(master);
        if (!renderer.editableBeforeClosing()
                || !renderer.usesStableMonthlyPath()) {
            throw new UnsupportedOperationException(
                    "この台帳は編集保存に対応していません。"
            );
        }
        boolean closed = closingRepository
                .findByTargetMonthAndDeletedAtIsNull(month.atDay(1))
                .map(entity ->
                        entity.getStatus()
                                == MonthlyClosingStatus.CLOSED
                )
                .orElse(false);
        if (closed && !renderer.editableAfterMonthlyClosing()) {
            throw new IllegalStateException(
                    targetMonth + " は締め済みのため編集できません。"
            );
        }

        editHandlerRegistry.find(renderer.rendererKey())
                .ifPresent(handler -> handler.apply(
                        targetMonth,
                        workbook
                ));

        byte[] data = serialize(workbook);
        String relativePath = relativePath(
                master,
                targetMonth,
                selectionValue
        );
        storageService.save(
                storageKeyResolver.resolve(
                        DocumentArea.GENERATED_REPORTS,
                        relativePath
                ),
                new ByteArrayInputStream(data),
                data.length,
                CONTENT_TYPE
        );
        return new SpreadsheetLedgerSaveResponse(
                relativePath,
                data.length,
                Instant.now(clock)
        );
    }

    private byte[] serialize(JsonNode workbook) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(workbook);
            if (data.length > MAX_WORKBOOK_BYTES) {
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

    private String relativePath(
            ExcelBookMaster master,
            String targetMonth,
            String selectionValue
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
        if (selectionValue != null) {
            directory += "selections/" + selectionValue + "/";
        }
        return directory
                + master.getBookCode()
                + "-"
                + targetMonth
                + ".json";
    }

    private void validateBookCode(String bookCode) {
        if (!StringUtils.hasText(bookCode)
                || !SAFE_SEGMENT.matcher(bookCode).matches()) {
            throw new IllegalArgumentException(
                    "bookCodeが不正です。"
            );
        }
    }

    private SpreadsheetLedgerRenderer renderer(
            ExcelBookMaster master
    ) {
        String rendererKey = StringUtils.hasText(master.getRendererKey())
                ? master.getRendererKey()
                : master.getLayoutType().name();
        return rendererRegistry.findRequired(rendererKey);
    }
}
