package com.project.backend.features.system.rule.service.fetcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.rule.entity.RuleColumnMapping;
import com.project.backend.features.system.rule.entity.RuleDataSource;
import com.project.backend.features.system.rule.entity.RuleDataSourceCatalog;
import com.project.backend.features.system.rule.enums.RuleDataType;
import com.project.backend.features.system.rule.service.RuleDataSourceCatalogService;

class GeneralDataFetcherTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private RuleDataSourceCatalogService catalogService;
    private GeneralDataFetcher fetcher;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        catalogService = mock(
                RuleDataSourceCatalogService.class
        );
        fetcher = new GeneralDataFetcher(
                jdbcTemplate,
                catalogService
        );
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void fetch_shouldUseCatalogAndForceTenantParameter() {
        RuleDataSourceCatalog catalog =
                new RuleDataSourceCatalog();
        catalog.setSourceCode("EMPLOYEE_BASIC");
        catalog.setPhysicalName("vw_rule_employee_basic");
        catalog.setWhereClauseTemplate(
                "tenant_id = :tenantId AND employee_id = :employeeId"
        );
        catalog.setTenantScopedFlag(true);
        catalog.setMaxRows(100);

        RuleColumnMapping column = new RuleColumnMapping();
        column.setColumnName("hourly_wage");
        column.setFactKey("hourlyWage");
        column.setDataType(RuleDataType.DECIMAL);

        RuleDataSource source = new RuleDataSource();
        source.setSourceName("employee");
        source.setCatalogCode("EMPLOYEE_BASIC");
        source.setSingleRowFlag(true);
        source.setColumns(List.of(column));

        when(catalogService.findRequired("EMPLOYEE_BASIC"))
                .thenReturn(catalog);
        when(jdbcTemplate.queryForList(
                anyString(),
                anyMap()
        )).thenReturn(List.of(Map.of(
                "hourly_wage",
                1500
        )));

        fetcher.fetch(source, Map.of(
                "tenantId",
                "spoofed",
                "employeeId",
                10L
        ));

        ArgumentCaptor<String> sqlCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> parameterCaptor =
                ArgumentCaptor.forClass((Class) Map.class);
        verify(jdbcTemplate).queryForList(
                sqlCaptor.capture(),
                parameterCaptor.capture()
        );

        assertThat(sqlCaptor.getValue())
                .isEqualTo(
                        "SELECT hourly_wage "
                                + "FROM vw_rule_employee_basic "
                                + "WHERE tenant_id = :tenantId "
                                + "AND employee_id = :employeeId "
                                + "LIMIT :__ruleLimit"
                );
        assertThat(parameterCaptor.getValue())
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("employeeId", 10L)
                .containsEntry("__ruleLimit", 2);
    }
}
