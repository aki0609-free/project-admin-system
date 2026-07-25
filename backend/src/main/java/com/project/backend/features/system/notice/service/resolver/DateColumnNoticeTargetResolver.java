package com.project.backend.features.system.notice.service.resolver;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.common.util.ApplicationDateUtils;
import com.project.backend.common.util.ApplicationDbUtils;
import com.project.backend.common.util.ApplicationValidationUtils;
import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.service.validation.NoticeSqlSafety;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DateColumnNoticeTargetResolver implements NoticeTargetResolver {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
    @Override
    public List<NoticeTargetRow> resolve(NoticeRule rule) {
        validateRule(rule);

        String keyColumn = rule.getTargetKeyColumnName();
        String dateColumn = rule.getTargetDateColumnName();
        String labelColumn = rule.getTargetLabelColumnName();

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ");
        sql.append(keyColumn).append(" AS target_key, ");
        sql.append(dateColumn).append(" AS target_date");

        if (StringUtils.hasText(labelColumn)) {
            sql.append(", ").append(labelColumn).append(" AS target_label");
        }

        sql.append(" FROM ").append(rule.getTargetTableName());
        sql.append(" WHERE tenant_id = :tenantId");
        sql.append(" AND ").append(dateColumn).append(" IS NOT NULL");

        if (StringUtils.hasText(rule.getWhereClause())) {
            NoticeSqlSafety.validateWhereClause(
                    rule.getWhereClause()
            );
            sql.append(" AND (").append(rule.getWhereClause()).append(")");
        }

        sql.append(" LIMIT 1001");

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        sql.toString(),
                        Map.of(
                                "tenantId",
                                requireTenantId()
                        )
                );

        if (rows.size() > 1000) {
            throw new IllegalArgumentException(
                    "Notice対象の取得件数が上限1000件を超えました。 ruleCode="
                            + rule.getRuleCode()
            );
        }

        return rows.stream()
                .map(this::toRow)
                .toList();
    }

    private NoticeTargetRow toRow(Map<String, Object> row) {
        return NoticeTargetRow.builder()
                .targetKey(ApplicationDbUtils.value(row, "target_key"))
                .targetDate(ApplicationDateUtils.toLocalDate(row.get("target_date")))
                .targetLabel(ApplicationDbUtils.value(row, "target_label"))
                .build();
    }

    private void validateRule(NoticeRule rule) {
        ApplicationValidationUtils.validateIdentifier(rule.getTargetTableName(), "targetTableName", SAFE_IDENTIFIER);
        ApplicationValidationUtils.validateIdentifier(rule.getTargetKeyColumnName(), "targetKeyColumnName", SAFE_IDENTIFIER);
        ApplicationValidationUtils.validateIdentifier(rule.getTargetDateColumnName(), "targetDateColumnName", SAFE_IDENTIFIER);

        if (StringUtils.hasText(rule.getTargetLabelColumnName())) {
            ApplicationValidationUtils.validateIdentifier(rule.getTargetLabelColumnName(), "targetLabelColumnName", SAFE_IDENTIFIER);
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();

        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException(
                    "Notice対象取得のtenantIdが未設定です。"
            );
        }

        return tenantId;
    }

}
