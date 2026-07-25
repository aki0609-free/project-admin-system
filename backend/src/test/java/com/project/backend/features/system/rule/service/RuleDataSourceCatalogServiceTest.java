package com.project.backend.features.system.rule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.entity.RuleDataSourceCatalog;
import com.project.backend.features.system.rule.entity.RuleDataSourceCatalogColumn;
import com.project.backend.features.system.rule.enums.RuleDataType;
import com.project.backend.features.system.rule.repository.RuleDataSourceCatalogRepository;

class RuleDataSourceCatalogServiceTest {

    @Test
    void findActive_shouldExposeOnlyActiveColumns() {
        RuleDataSourceCatalogRepository repository =
                mock(RuleDataSourceCatalogRepository.class);
        RuleDataSourceCatalogService service =
                new RuleDataSourceCatalogService(repository);

        RuleDataSourceCatalog catalog =
                new RuleDataSourceCatalog();
        catalog.setSourceCode("EMPLOYEE_BASIC");
        catalog.setDisplayName("従業員基本情報");
        catalog.setMaxRows(100);
        catalog.setActiveFlag(true);

        RuleDataSourceCatalogColumn active =
                new RuleDataSourceCatalogColumn();
        active.setColumnName("employee_id");
        active.setDisplayName("従業員ID");
        active.setDataType(RuleDataType.LONG);
        active.setOrderNo(1);
        active.setActiveFlag(true);

        RuleDataSourceCatalogColumn inactive =
                new RuleDataSourceCatalogColumn();
        inactive.setColumnName("secret_value");
        inactive.setDisplayName("非公開");
        inactive.setDataType(RuleDataType.STRING);
        inactive.setOrderNo(2);
        inactive.setActiveFlag(false);

        catalog.setColumns(List.of(inactive, active));

        when(repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderBySourceCodeAsc())
                .thenReturn(List.of(catalog));

        assertThat(service.findActive())
                .singleElement()
                .satisfies(response ->
                        assertThat(response.columns())
                                .singleElement()
                                .satisfies(column ->
                                        assertThat(
                                                column.columnName()
                                        ).isEqualTo("employee_id")));
    }
}
