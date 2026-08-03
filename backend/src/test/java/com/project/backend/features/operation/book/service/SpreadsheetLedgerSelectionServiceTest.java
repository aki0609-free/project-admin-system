package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalogColumn;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookGenerationUnit;
import com.project.backend.features.system.excelbook.enums.ExcelBookSelectionMode;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;
import com.project.backend.features.system.excelbook.service.ExcelBookDataSourceCatalogService;

class SpreadsheetLedgerSelectionServiceTest {

    private ExcelBookMasterRepository masterRepository;
    private ExcelBookDataSourceCatalogService catalogService;
    private ExcelBookDataSourceRowQueryService rowQueryService;
    private SpreadsheetLedgerSelectionService service;

    @BeforeEach
    void setUp() {
        masterRepository = mock(ExcelBookMasterRepository.class);
        catalogService = mock(ExcelBookDataSourceCatalogService.class);
        rowQueryService = mock(ExcelBookDataSourceRowQueryService.class);
        service = new SpreadsheetLedgerSelectionService(
                masterRepository,
                catalogService,
                rowQueryService
        );
    }

    @Test
    void find_shouldBuildDistinctOptionsFromAllowedCatalog() {
        ExcelBookMaster master = new ExcelBookMaster();
        master.setSelectionMode(ExcelBookSelectionMode.MULTIPLE);
        master.setSelectionSourceName("EMPLOYEE_SELECTOR");
        master.setSelectionValueColumn("employee_id");
        master.setSelectionDisplayColumns(
                "employee_code,employee_name,payment_cycle"
        );
        master.setAllowSelectAll(true);
        master.setGenerationUnit(
                ExcelBookGenerationUnit.FILE_PER_SELECTION
        );

        ExcelBookDataSourceCatalog catalog =
                new ExcelBookDataSourceCatalog();
        addColumn(catalog, "employee_id", "ID", "NUMBER", 1);
        addColumn(catalog, "employee_code", "従業員番号", "STRING", 2);
        addColumn(catalog, "employee_name", "氏名", "STRING", 3);
        addColumn(catalog, "payment_cycle", "支払区分", "STRING", 4);

        when(masterRepository
                .findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        "MONTHLY_LABOR"
                )).thenReturn(Optional.of(master));
        when(catalogService.findRequired("EMPLOYEE_SELECTOR"))
                .thenReturn(catalog);
        when(rowQueryService.findAllRows(catalog, "2026-07"))
                .thenReturn(List.of(
                        Map.of(
                                "employee_id", 3L,
                                "employee_code", "0003",
                                "employee_name", "中山 誠一",
                                "payment_cycle", "MONTHLY"
                        ),
                        Map.of(
                                "employee_id", 3L,
                                "employee_code", "0003",
                                "employee_name", "中山 誠一",
                                "payment_cycle", "MONTHLY"
                        )
                ));

        var result = service.find("MONTHLY_LABOR", "2026-07");

        assertThat(result.mode()).isEqualTo(
                ExcelBookSelectionMode.MULTIPLE
        );
        assertThat(result.columns()).extracting(
                column -> column.columnName()
        ).containsExactly(
                "employee_code",
                "employee_name",
                "payment_cycle"
        );
        assertThat(result.options()).hasSize(1);
        assertThat(result.options().getFirst().value()).isEqualTo("3");
        assertThat(result.options().getFirst().displayValues())
                .containsEntry("employee_name", "中山 誠一");
    }

    private void addColumn(
            ExcelBookDataSourceCatalog catalog,
            String name,
            String displayName,
            String dataType,
            int orderNo
    ) {
        ExcelBookDataSourceCatalogColumn column =
                new ExcelBookDataSourceCatalogColumn();
        column.setCatalog(catalog);
        column.setColumnName(name);
        column.setDisplayName(displayName);
        column.setDataType(dataType);
        column.setOrderNo(orderNo);
        column.setActiveFlag(true);
        catalog.getColumns().add(column);
    }
}
