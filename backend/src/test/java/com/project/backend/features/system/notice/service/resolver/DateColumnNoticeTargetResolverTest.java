package com.project.backend.features.system.notice.service.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.notice.entity.NoticeRule;

class DateColumnNoticeTargetResolverTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private DateColumnNoticeTargetResolver resolver;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        resolver = new DateColumnNoticeTargetResolver(
                jdbcTemplate
        );
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void resolve_shouldForceTenantConditionAndLimit() {
        NoticeRule rule = new NoticeRule();
        rule.setRuleCode("CUSTOMER_CLOSING_NOTICE");
        rule.setTargetTableName("customers");
        rule.setTargetKeyColumnName("id");
        rule.setTargetDateColumnName("closing_date");
        rule.setTargetLabelColumnName("name");
        rule.setWhereClause("active_flag = 1");

        when(jdbcTemplate.queryForList(
                anyString(),
                anyMap()
        )).thenReturn(List.of(Map.of(
                "target_key",
                10L,
                "target_date",
                Date.valueOf("2026-07-31"),
                "target_label",
                "株式会社A"
        )));

        var result = resolver.resolve(rule);

        assertThat(result)
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.targetKey()).isEqualTo("10");
                    assertThat(row.targetDate())
                            .isEqualTo(
                                    LocalDate.of(2026, 7, 31)
                            );
                });

        ArgumentCaptor<String> sqlCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor =
                ArgumentCaptor.forClass((Class) Map.class);
        verify(jdbcTemplate).queryForList(
                sqlCaptor.capture(),
                paramsCaptor.capture()
        );

        assertThat(sqlCaptor.getValue())
                .contains("WHERE tenant_id = :tenantId")
                .contains("LIMIT 1001");
        assertThat(paramsCaptor.getValue())
                .containsEntry("tenantId", "tenant-a");
    }
}
