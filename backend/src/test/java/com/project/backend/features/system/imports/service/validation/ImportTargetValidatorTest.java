package com.project.backend.features.system.imports.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.imports.dto.ImportColumnSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.entity.ImportTargetCatalog;
import com.project.backend.features.system.imports.entity.ImportTargetCatalogColumn;
import com.project.backend.features.system.imports.enums.ImportDataType;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;
import com.project.backend.features.system.imports.service.ImportTargetCatalogService;

class ImportTargetValidatorTest {

    private ImportTargetRepository repository;
    private ImportTargetCatalogService catalogService;
    private ImportTargetValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(ImportTargetRepository.class);
        catalogService = mock(ImportTargetCatalogService.class);
        validator = new ImportTargetValidator(
                repository,
                catalogService
        );
    }

    @Test
    void カタログ未許可のdeleteInsertを拒否する() {
        ImportTargetCatalog catalog = catalog(false);
        when(catalogService.findRequired("employee_import"))
                .thenReturn(catalog);

        assertThatThrownBy(() ->
                validator.validateForCreate(
                        request(ImportMode.DELETE_INSERT, true)
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DELETE_INSERT");
    }

    @Test
    void updateOnlyでキー未設定を拒否する() {
        ImportTargetCatalog catalog = catalog(true);
        when(catalogService.findRequired("employee_import"))
                .thenReturn(catalog);

        assertThatThrownBy(() ->
                validator.validateForCreate(
                        request(ImportMode.UPDATE_ONLY, false)
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("キー項目");
    }

    @Test
    void カタログと異なるデータ型を拒否する() {
        ImportTargetCatalog catalog = catalog(true);
        when(catalogService.findRequired("employee_import"))
                .thenReturn(catalog);

        ImportColumnSaveRequest invalidColumn =
                column(true, ImportDataType.INTEGER);

        ImportTargetSaveRequest request =
                baseRequest(
                        ImportMode.UPSERT,
                        List.of(invalidColumn)
                );

        assertThatThrownBy(() ->
                validator.validateForCreate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("データ型");
    }

    @Test
    void upload時のPython前処理を許可する() {
        ImportTargetCatalog catalog = catalog(true);
        when(catalogService.findRequired("employee_import"))
                .thenReturn(catalog);

        ImportTargetSaveRequest request = ImportTargetSaveRequest.builder()
                .targetCode("IMPORT_EMPLOYEE")
                .targetName("従業員取込")
                .tableName("employee_import")
                .sourceType(ImportSourceType.UPLOAD)
                .fixedFilePath("normalized.csv")
                .scriptType(ImportScriptType.PYTHON)
                .scriptPath("normalize.py")
                .scriptArgs("--input ${IMPORT_INPUT_FILE} --output ${IMPORT_CSV_DIR}/normalized.csv")
                .importMode(ImportMode.UPSERT)
                .headerRowNumber(1)
                .dataStartRowNumber(2)
                .charset("UTF-8")
                .delimiter(",")
                .columns(List.of(column(true, ImportDataType.STRING)))
                .build();

        assertThatCode(() -> validator.validateForCreate(request))
                .doesNotThrowAnyException();
    }

    private ImportTargetSaveRequest request(
            ImportMode mode,
            boolean keyFlag
    ) {
        return baseRequest(
                mode,
                List.of(column(keyFlag, ImportDataType.STRING))
        );
    }

    private ImportTargetSaveRequest baseRequest(
            ImportMode mode,
            List<ImportColumnSaveRequest> columns
    ) {
        return ImportTargetSaveRequest.builder()
                .targetCode("IMPORT_EMPLOYEE")
                .targetName("従業員取込")
                .tableName("employee_import")
                .sourceType(ImportSourceType.UPLOAD)
                .scriptType(ImportScriptType.NONE)
                .importMode(mode)
                .headerRowNumber(1)
                .dataStartRowNumber(2)
                .charset("UTF-8")
                .delimiter(",")
                .columns(columns)
                .build();
    }

    private ImportColumnSaveRequest column(
            boolean keyFlag,
            ImportDataType dataType
    ) {
        return ImportColumnSaveRequest.builder()
                .columnName("employee_code")
                .csvHeaderName("employeeCode")
                .dataType(dataType)
                .keyFlag(keyFlag)
                .orderNo(1)
                .build();
    }

    private ImportTargetCatalog catalog(
            boolean allowDeleteInsert
    ) {
        ImportTargetCatalog catalog = new ImportTargetCatalog();
        catalog.setTableName("employee_import");
        catalog.setAllowDeleteInsertFlag(allowDeleteInsert);

        ImportTargetCatalogColumn column =
                new ImportTargetCatalogColumn();
        column.setColumnName("employee_code");
        column.setDataType(ImportDataType.STRING);
        column.setActiveFlag(true);
        catalog.addColumn(column);

        return catalog;
    }
}
