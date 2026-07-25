package com.project.backend.features.system.notice.service.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeRule;

class DayRuleNoticeTargetResolverTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private DayRuleNoticeTargetResolver resolver;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);

        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-01-31T15:30:00Z"),
                ZoneId.of("Asia/Tokyo")
        );

        resolver = new DayRuleNoticeTargetResolver(
                jdbcTemplate,
                fixedClock
        );
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolve_shouldUseTokyoBusinessMonthAcrossUtcMonthBoundary() {
        when(jdbcTemplate.queryForList(
                anyString(),
                anyMap()
        )).thenReturn(List.of(Map.of(
                "target_key", 1L,
                "day_rule_type", "DAY_OF_MONTH",
                "day_rule_value", 31,
                "target_label", "月末対象"
        )));

        List<NoticeTargetRow> result =
                resolver.resolve(rule());

        assertThat(result).singleElement()
                .satisfies(row -> {
                    assertThat(row.targetKey()).isEqualTo("1");
                    assertThat(row.targetDate())
                            .isEqualTo(LocalDate.of(2026, 2, 28));
                    assertThat(row.targetLabel())
                            .isEqualTo("月末対象");
                });
    }

    private NoticeRule rule() {
        NoticeRule rule = new NoticeRule();
        rule.setRuleCode("MONTH_END_NOTICE");
        rule.setTargetTableName("customers");
        rule.setTargetKeyColumnName("id");
        rule.setTargetDayTypeColumnName("closing_day_type");
        rule.setTargetDayValueColumnName("closing_day_value");
        rule.setTargetLabelColumnName("name");
        return rule;
    }
}
