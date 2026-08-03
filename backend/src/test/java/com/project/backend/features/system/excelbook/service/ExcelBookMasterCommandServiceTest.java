package com.project.backend.features.system.excelbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterRequest;
import com.project.backend.features.system.excelbook.dto.ExcelBookVariableMappingRequest;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalogColumn;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;
import com.project.backend.features.system.excelbook.mapper.ExcelBookMasterMapper;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

class ExcelBookMasterCommandServiceTest {

    private ExcelBookMasterRepository repository;
    private ExcelBookMasterCommandService service;
    private ExcelBookDataSourceCatalogService catalogService;

    @BeforeEach
    void setUp() {
        repository = mock(ExcelBookMasterRepository.class);
        catalogService = mock(ExcelBookDataSourceCatalogService.class);
        service = new ExcelBookMasterCommandService(
                repository,
                new ExcelBookMasterMapper(),
                catalogService
        );
        ExcelBookDataSourceCatalog catalog =
                new ExcelBookDataSourceCatalog();
        catalog.setSourceCode("MONTHLY_SITE_SUMMARY");
        ExcelBookDataSourceCatalogColumn column =
                new ExcelBookDataSourceCatalogColumn();
        column.setCatalog(catalog);
        column.setColumnName("employee_name");
        column.setActiveFlag(true);
        catalog.getColumns().add(column);
        when(catalogService.findRequired("MONTHLY_SITE_SUMMARY"))
                .thenReturn(catalog);
    }

    @Test
    void create_shouldInitializeLegacyExcelPathsWithoutExposingThem()
            throws Exception {
        when(repository.existsByBookCodeAndDeletedAtIsNull(
                "MONTHLY_LEDGER"
        )).thenReturn(false);
        when(repository.save(any(ExcelBookMaster.class)))
                .thenAnswer(invocation -> {
                    ExcelBookMaster entity = invocation.getArgument(0);
                    entity.setId(42L);
                    return entity;
                });

        Long id = service.create(request("MONTHLY_LEDGER"));

        ArgumentCaptor<ExcelBookMaster> captor =
                ArgumentCaptor.forClass(ExcelBookMaster.class);
        verify(repository).save(captor.capture());
        assertThat(id).isEqualTo(42L);
        assertThat(captor.getValue().getTemplateFilePath()).isEmpty();
        assertThat(captor.getValue().getOutputFilePath()).isEmpty();
    }

    @Test
    void update_shouldRejectChangingBookCode() {
        ExcelBookMaster entity = master("MONTHLY_LEDGER");
        when(repository.findByIdAndDeletedAtIsNull(42L))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                service.update(42L, request("OTHER_LEDGER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("作成後に変更できません");

        verify(repository, never()).save(any());
    }

    @Test
    void delete_shouldSoftDeleteAndKeepTemplateRecoverable() {
        ExcelBookMaster entity = master("MONTHLY_LEDGER");
        when(repository.findByIdAndDeletedAtIsNull(42L))
                .thenReturn(Optional.of(entity));

        service.delete(42L);

        assertThat(entity.getDeletedAt()).isNotNull();
        assertThat(entity.getActiveFlag()).isFalse();
        verify(repository).save(entity);
        verify(repository, never()).delete(any());
    }

    @Test
    void create_shouldRejectPathUnsafeBookCode() {
        assertThatThrownBy(() ->
                service.create(request("../MONTHLY_LEDGER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("半角英大文字");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldRejectColumnOutsideCatalog() {
        ExcelBookMasterRequest request = request(
                "MONTHLY_LEDGER",
                new ExcelBookVariableMappingRequest(
                        "rows.salary",
                        "salary",
                        "ROW",
                        "NUMBER",
                        1
                )
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("許可されていない");
    }

    @Test
    void create_shouldRejectDuplicateVariableKey() {
        ExcelBookVariableMappingRequest first =
                new ExcelBookVariableMappingRequest(
                        "rows.employeeName",
                        "employee_name",
                        "ROW",
                        "STRING",
                        1
                );
        ExcelBookVariableMappingRequest second =
                new ExcelBookVariableMappingRequest(
                        "rows.employeeName",
                        "employee_name",
                        "ROW",
                        "STRING",
                        2
                );

        assertThatThrownBy(() ->
                service.create(request(
                        "MONTHLY_LEDGER",
                        first,
                        second
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重複");
    }

    private ExcelBookMasterRequest request(String bookCode) {
        return request(bookCode, new ExcelBookVariableMappingRequest[0]);
    }

    private ExcelBookMasterRequest request(
            String bookCode,
            ExcelBookVariableMappingRequest... mappings
    ) {
        return new ExcelBookMasterRequest(
                bookCode,
                "月次台帳",
                ExcelBookSourceType.SNAPSHOT,
                "MONTHLY_SITE_SUMMARY",
                "TEMPLATE",
                true,
                java.util.List.of(mappings)
        );
    }

    private ExcelBookMaster master(String bookCode) {
        ExcelBookMaster entity = new ExcelBookMaster();
        entity.setId(42L);
        entity.setBookCode(bookCode);
        entity.setBookName("月次台帳");
        entity.setActiveFlag(true);
        return entity;
    }
}
