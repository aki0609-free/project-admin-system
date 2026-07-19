package com.project.backend.features.system.notice.service.resolver;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.common.dayrule.utils.DayRuleUtils;
import com.project.backend.common.util.ApplicationDbUtils;
import com.project.backend.common.util.ApplicationNumberUtils;
import com.project.backend.common.util.ApplicationValidationUtils;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.converters.NoticeEnumConverter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DayRuleNoticeTargetResolver implements NoticeTargetResolver {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
    @Override
    public List<NoticeTargetRow> resolve(NoticeRule rule) {
        validateRule(rule);

        String keyColumn = rule.getTargetKeyColumnName();
        String typeColumn = rule.getTargetDayTypeColumnName();
        String valueColumn = rule.getTargetDayValueColumnName();
        String labelColumn = rule.getTargetLabelColumnName();

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ");
        sql.append(keyColumn).append(" AS target_key, ");
        sql.append(typeColumn).append(" AS day_rule_type, ");
        sql.append(valueColumn).append(" AS day_rule_value");

        if (StringUtils.hasText(labelColumn)) {
            sql.append(", ").append(labelColumn).append(" AS target_label");
        }

        sql.append(" FROM ").append(rule.getTargetTableName());
        sql.append(" WHERE ").append(typeColumn).append(" IS NOT NULL");

        if (StringUtils.hasText(rule.getWhereClause())) {
            sql.append(" AND (").append(rule.getWhereClause()).append(")");
        }

        YearMonth targetYearMonth = YearMonth.now();

        return jdbcTemplate.queryForList(sql.toString(), Map.of()).stream()
                .map(row -> toRow(row, targetYearMonth))
                .toList();
    }

    private NoticeTargetRow toRow(
            Map<String, Object> row,
            YearMonth targetYearMonth
    ) {
        DayRuleType type = NoticeEnumConverter.toDayRuleType(row.get("day_rule_type"));
        Integer value = ApplicationNumberUtils.toInteger(row.get("day_rule_value"));

        LocalDate targetDate = DayRuleUtils.resolve(
                type,
                value,
                targetYearMonth
        );

        return NoticeTargetRow.builder()
                .targetKey(ApplicationDbUtils.value(row, "target_key"))
                .targetDate(targetDate)
                .targetLabel(ApplicationDbUtils.value(row, "target_label"))
                .build();
    }

    private void validateRule(NoticeRule rule) {
        ApplicationValidationUtils.validateIdentifier(rule.getTargetTableName(), "targetTableName", SAFE_IDENTIFIER);
        ApplicationValidationUtils.validateIdentifier(rule.getTargetKeyColumnName(), "targetKeyColumnName", SAFE_IDENTIFIER);
        ApplicationValidationUtils.validateIdentifier(rule.getTargetDayTypeColumnName(), "targetDayTypeColumnName", SAFE_IDENTIFIER);
        ApplicationValidationUtils.validateIdentifier(rule.getTargetDayValueColumnName(), "targetDayValueColumnName", SAFE_IDENTIFIER);

        if (StringUtils.hasText(rule.getTargetLabelColumnName())) {
            ApplicationValidationUtils.validateIdentifier(rule.getTargetLabelColumnName(), "targetLabelColumnName", SAFE_IDENTIFIER);
        }
    }

}