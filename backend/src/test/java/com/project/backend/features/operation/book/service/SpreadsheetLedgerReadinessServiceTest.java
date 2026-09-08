package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateResponse;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalogColumn;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookLayoutType;
import com.project.backend.features.system.excelbook.service.ExcelBookDataSourceCatalogService;
import com.project.backend.features.system.excelbook.service.SpreadsheetTemplateService;

class SpreadsheetLedgerReadinessServiceTest {

    private SpreadsheetLedgerRendererRegistry rendererRegistry;
    private ExcelBookDataSourceCatalogService catalogService;
    private SpreadsheetTemplateService templateService;
    private MonthlyClosingOutputDefinitionRepository closingRepository;
    private SpreadsheetLedgerReadinessService service;

    @BeforeEach
    void setUp() {
        rendererRegistry = mock(SpreadsheetLedgerRendererRegistry.class);
        catalogService = mock(ExcelBookDataSourceCatalogService.class);
        templateService = mock(SpreadsheetTemplateService.class);
        closingRepository = mock(
                MonthlyClosingOutputDefinitionRepository.class
        );
        service = new SpreadsheetLedgerReadinessService(
                rendererRegistry,
                catalogService,
                templateService,
                closingRepository
        );
    }

    @Test
    void assess_shouldConfirmSystemMasterTemplateAndClosingLink() {
        ExcelBookMaster master = master();
        SpreadsheetLedgerRenderer renderer = mock(
                SpreadsheetLedgerRenderer.class
        );
        when(renderer.requiresTemplate()).thenReturn(true);
        when(rendererRegistry.findRequired("REPEATING_ROW"))
                .thenReturn(renderer);
        when(catalogService.findRequired("LEDGER_SOURCE"))
                .thenReturn(catalog("employee_name"));
        when(templateService.find(10L)).thenReturn(
                new SpreadsheetTemplateResponse(
                        10L,
                        "EMPLOYEE_LEDGER",
                        "ledgers/default/EMPLOYEE_LEDGER/template.json",
                        new ObjectMapper().createObjectNode()
                )
        );
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setActiveFlag(true);
        when(closingRepository
                .findByTenantIdAndOutputTypeAndOutputCodeAndDeletedAtIsNull(
                        "default",
                        MonthlyClosingOutputType.LEDGER,
                        "EMPLOYEE_LEDGER"
                )).thenReturn(Optional.of(definition));

        var result = service.assess(master);

        assertThat(result.generationReady()).isTrue();
        assertThat(result.templateRequired()).isTrue();
        assertThat(result.templateConfigured()).isTrue();
        assertThat(result.monthlyClosingConfigured()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void assess_shouldReturnAllMissingDependenciesWithoutHidingMaster() {
        ExcelBookMaster master = master();
        when(rendererRegistry.findRequired("REPEATING_ROW"))
                .thenThrow(new IllegalArgumentException("missing"));
        when(catalogService.findRequired("LEDGER_SOURCE"))
                .thenThrow(new IllegalArgumentException("missing"));

        var result = service.assess(master);

        assertThat(result.generationReady()).isFalse();
        assertThat(result.monthlyClosingConfigured()).isFalse();
        assertThat(result.issues())
                .anyMatch(message -> message.contains("描画方式"))
                .anyMatch(message -> message.contains("データソース"));
    }

    private ExcelBookMaster master() {
        ExcelBookMaster master = new ExcelBookMaster();
        master.setId(10L);
        master.setTenantId("default");
        master.setBookCode("EMPLOYEE_LEDGER");
        master.setBookName("従業員台帳");
        master.setLayoutType(ExcelBookLayoutType.REPEATING_ROW);
        master.setRendererKey("REPEATING_ROW");
        master.setSourceName("LEDGER_SOURCE");
        return master;
    }

    private ExcelBookDataSourceCatalog catalog(String... columnNames) {
        ExcelBookDataSourceCatalog catalog =
                new ExcelBookDataSourceCatalog();
        for (String columnName : columnNames) {
            ExcelBookDataSourceCatalogColumn column =
                    new ExcelBookDataSourceCatalogColumn();
            column.setColumnName(columnName);
            column.setActiveFlag(true);
            catalog.getColumns().add(column);
        }
        return catalog;
    }
}
