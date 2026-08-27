package com.project.backend.features.system.excelbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateSaveRequest;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

class SpreadsheetTemplateServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ExcelBookMasterRepository repository;
    private StorageService storageService;
    private SpreadsheetTemplateService service;
    private ExcelBookTemplateRequirementResolver templateRequirementResolver;
    private ExcelBookMaster master;

    @BeforeEach
    void setUp() {
        repository = mock(ExcelBookMasterRepository.class);
        storageService = mock(StorageService.class);
        templateRequirementResolver = mock(
                ExcelBookTemplateRequirementResolver.class
        );
        when(templateRequirementResolver.requiresTemplate(any()))
                .thenReturn(true);
        service = new SpreadsheetTemplateService(
                repository,
                storageService,
                new DocumentStorageKeyResolver(new StorageProperties()),
                objectMapper,
                templateRequirementResolver
        );

        master = new ExcelBookMaster();
        master.setId(42L);
        master.setBookCode("MONTHLY_LEDGER");
        master.setTenantId("tenant-a");
        master.setLayoutType(
                com.project.backend.features.system.excelbook.enums
                        .ExcelBookLayoutType.REPEATING_ROW
        );
        master.setRendererKey("REPEATING_ROW");

        when(repository.findByIdAndDeletedAtIsNull(42L))
                .thenReturn(Optional.of(master));
    }

    @Test
    void find_shouldReturnEmptyTemplateWhenStorageObjectDoesNotExist() {
        when(storageService.exists(storageKey())).thenReturn(false);

        var result = service.find(42L);

        assertThat(result.masterId()).isEqualTo(42L);
        assertThat(result.bookCode()).isEqualTo("MONTHLY_LEDGER");
        assertThat(result.storagePath())
                .isEqualTo(
                        "ledgers/tenant-a/MONTHLY_LEDGER/template.json"
                );
        assertThat(result.workbook()).isNull();
    }

    @Test
    void find_shouldLoadWorkbookJsonFromManagedTemplateArea()
            throws Exception {
        JsonNode workbook = objectMapper.readTree(
                """
                {"sheets":[{"name":"TEMPLATE","rows":[]}]}
                """
        );
        when(storageService.exists(storageKey())).thenReturn(true);
        when(storageService.load(storageKey())).thenReturn(
                new ByteArrayInputStream(
                        objectMapper.writeValueAsBytes(workbook)
                )
        );

        var result = service.find(42L);

        assertThat(result.workbook()).isEqualTo(workbook);
    }

    @Test
    void save_shouldWriteWorkbookJsonToManagedTemplateArea()
            throws Exception {
        JsonNode workbook = objectMapper.readTree(
                """
                {"sheets":[{"name":"TEMPLATE","rows":[{"cells":[{"formula":"=1+1"}]}]}]}
                """
        );

        var result = service.save(
                42L,
                new SpreadsheetTemplateSaveRequest(workbook)
        );

        verify(storageService).save(
                eq(storageKey()),
                any(InputStream.class),
                eq((long) objectMapper.writeValueAsBytes(workbook).length),
                eq("application/json")
        );
        assertThat(result.workbook()).isEqualTo(workbook);
    }

    @Test
    void save_shouldRejectNonObjectWorkbook() throws Exception {
        JsonNode workbook = objectMapper.readTree("[]");

        assertThatThrownBy(() ->
                service.save(
                        42L,
                        new SpreadsheetTemplateSaveRequest(workbook)
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSONオブジェクト");

        verifyNoInteractions(storageService);
    }

    @Test
    void save_shouldRejectTemplateLargerThanTenMegabytes() {
        String largeValue = "a".repeat(10 * 1024 * 1024);
        JsonNode workbook = objectMapper.createObjectNode()
                .put("value", largeValue);

        assertThatThrownBy(() ->
                service.save(
                        42L,
                        new SpreadsheetTemplateSaveRequest(workbook)
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10MB");

        verifyNoInteractions(storageService);
    }

    @Test
    void save_shouldRejectTemplateForCodeGeneratedLedger()
            throws Exception {
        when(templateRequirementResolver.requiresTemplate(
                "REPEATING_ROW"
        )).thenReturn(false);
        JsonNode workbook = objectMapper.readTree(
                """
                {"sheets":[]}
                """
        );

        assertThatThrownBy(() -> service.save(
                42L,
                new SpreadsheetTemplateSaveRequest(workbook)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("コード生成台帳");

        verifyNoInteractions(storageService);
    }

    private String storageKey() {
        return "documents/templates/ledgers/tenant-a/MONTHLY_LEDGER/template.json";
    }
}
