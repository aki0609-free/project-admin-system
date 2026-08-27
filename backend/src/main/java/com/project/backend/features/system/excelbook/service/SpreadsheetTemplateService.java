package com.project.backend.features.system.excelbook.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            return response(master, relativePath, workbook);
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
