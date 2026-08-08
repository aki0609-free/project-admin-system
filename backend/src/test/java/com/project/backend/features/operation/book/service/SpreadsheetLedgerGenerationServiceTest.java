package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateResponse;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerGenerationMode;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.entity.ExcelBookVariableMapping;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;
import com.project.backend.features.system.excelbook.service.ExcelBookDataSourceCatalogService;
import com.project.backend.features.system.excelbook.service.SpreadsheetTemplateService;

class SpreadsheetLedgerGenerationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExcelBookMasterRepository repository;
    private ExcelBookDataSourceCatalogService catalogService;
    private SpreadsheetTemplateService templateService;
    private ExcelBookDataSourceRowQueryService rowQueryService;
    private SpreadsheetWorkbookTemplateExpander expander;
    private MonthlyClosingRepository closingRepository;
    private StorageService storageService;
    private SpreadsheetLedgerGenerationService service;
    private ExcelBookMaster master;

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(ExcelBookMasterRepository.class);
        catalogService = mock(
                ExcelBookDataSourceCatalogService.class
        );
        templateService = mock(SpreadsheetTemplateService.class);
        rowQueryService = mock(
                ExcelBookDataSourceRowQueryService.class
        );
        expander = mock(SpreadsheetWorkbookTemplateExpander.class);
        closingRepository = mock(MonthlyClosingRepository.class);
        storageService = mock(StorageService.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-07-28T12:34:56.789Z"),
                ZoneId.of("Asia/Tokyo")
        );
        service = new SpreadsheetLedgerGenerationService(
                repository,
                catalogService,
                templateService,
                rowQueryService,
                new SpreadsheetLedgerRendererRegistry(
                        List.of(
                                new RepeatingRowSpreadsheetRenderer(
                                        expander
                                ),
                                new MonthlyLaborSpreadsheetRenderer(
                                        objectMapper
                                )
                        )
                ),
                mock(SpreadsheetLedgerSelectionService.class),
                closingRepository,
                storageService,
                new DocumentStorageKeyResolver(
                        new StorageProperties()
                ),
                objectMapper,
                clock
        );

        master = new ExcelBookMaster();
        master.setId(42L);
        master.setBookCode("EMPLOYEE_LEDGER");
        master.setBookName("従業員台帳");
        master.setTenantId("tenant-a");
        master.setSourceType(ExcelBookSourceType.SNAPSHOT);
        master.setSourceName("EMPLOYEE_LEDGER_SOURCE");
        ExcelBookVariableMapping mapping =
                new ExcelBookVariableMapping();
        mapping.setVariableKey("rows.employeeName");
        mapping.setSourceColumn("employee_name");
        mapping.setScope("ROW");
        mapping.setDataType("STRING");
        master.addVariableMapping(mapping);

        JsonNode template = objectMapper.readTree(
                """
                {"sheets":[{"rows":[]}]}
                """
        );
        JsonNode generated = objectMapper.readTree(
                """
                {"sheets":[{"rows":[{"cells":[{"value":"山田 太郎"}]}]}]}
                """
        );
        ExcelBookDataSourceCatalog catalog =
                new ExcelBookDataSourceCatalog();
        var rows = List.<Map<String, Object>>of(
                Map.of("employee_name", "山田 太郎")
        );

        when(repository
                .findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        "EMPLOYEE_LEDGER"
                ))
                .thenReturn(Optional.of(master));
        when(catalogService.findRequired("EMPLOYEE_LEDGER_SOURCE"))
                .thenReturn(catalog);
        when(rowQueryService.findRows(
                catalog,
                master.getVariableMappings(),
                "2026-07"
        )).thenReturn(rows);
        when(templateService.find(42L)).thenReturn(
                new SpreadsheetTemplateResponse(
                        42L,
                        "EMPLOYEE_LEDGER",
                        "ledgers/tenant-a/EMPLOYEE_LEDGER/template.json",
                        template
                )
        );
        when(expander.expand(
                template,
                master,
                rows,
                "2026-07",
                Instant.parse("2026-07-28T12:34:56.789Z")
        )).thenReturn(generated);
    }

    @Test
    void generate_shouldSaveWorkbookToGeneratedReportsArea()
            throws Exception {
        var result = service.generate(
                "EMPLOYEE_LEDGER",
                "2026-07"
        );

        String relativePath =
                "ledgers/tenant-a/EMPLOYEE_LEDGER/2026-07/"
                        + "EMPLOYEE_LEDGER-2026-07-"
                        + "20260728-213456789.json";
        String storageKey =
                "documents/generated-reports/" + relativePath;

        assertThat(result.rowCount()).isEqualTo(1);
        assertThat(result.generatedAt()).isEqualTo(
                Instant.parse("2026-07-28T12:34:56.789Z")
        );
        assertThat(result.storagePath()).isEqualTo(relativePath);
        verify(storageService).save(
                eq(storageKey),
                any(InputStream.class),
                eq((long) objectMapper.writeValueAsBytes(
                        result.workbook()
                ).length),
                eq("application/json")
        );
    }

    @Test
    void findActive_shouldDistinguishTemplateAndCodeGeneration() {
        ExcelBookMaster codeGenerated = new ExcelBookMaster();
        codeGenerated.setId(43L);
        codeGenerated.setBookCode("MONTHLY_LABOR");
        codeGenerated.setBookName("月間労務表");
        codeGenerated.setSourceName("MONTHLY_LABOR_SOURCE");
        codeGenerated.setRendererKey(MonthlyLaborSpreadsheetRenderer.KEY);

        when(repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByBookNameAsc())
                .thenReturn(List.of(master, codeGenerated));

        var result = service.findActive();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).generationMode())
                .isEqualTo(SpreadsheetLedgerGenerationMode.TEMPLATE);
        assertThat(result.get(0).generationReady()).isTrue();
        assertThat(result.get(0).templateConfigured()).isTrue();
        assertThat(result.get(1).generationMode())
                .isEqualTo(SpreadsheetLedgerGenerationMode.CODE);
        assertThat(result.get(1).generationReady()).isTrue();
        assertThat(result.get(1).templateConfigured()).isFalse();
    }
}
