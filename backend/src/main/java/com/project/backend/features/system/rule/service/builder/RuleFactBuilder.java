package com.project.backend.features.system.rule.service.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.entity.RuleColumnMapping;
import com.project.backend.features.system.rule.entity.RuleDataSource;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.service.fetcher.GeneralDataFetcher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleFactBuilder {

    private final GeneralDataFetcher dataFetcher;

    public Map<String, Object> build(
            RuleMaster rule,
            RuleContextRequest contextRequest
    ) {
        Map<String, Object> facts = new LinkedHashMap<>();
        Map<String, Object> params = toParameterMap(contextRequest);

        facts.put("params", params);
        facts.putAll(params);

        if (rule.getDataSources() == null) {
            return facts;
        }

        rule.getDataSources().stream()
                .filter(source -> source.getDeletedAt() == null)
                .filter(RuleDataSource::isActiveFlag)
                .sorted((a, b) -> Integer.compare(a.getOrderNo(), b.getOrderNo()))
                .forEach(source ->
                        putSourceFacts(
                                facts,
                                source,
                                params
                        )
                );

        return facts;
    }

    private void putSourceFacts(
            Map<String, Object> facts,
            RuleDataSource source,
            Map<String, Object> params
    ) {
        List<Map<String, Object>> rows =
                dataFetcher.fetch(
                        source,
                        params
                );

        if (source.isSingleRowFlag()) {
            Map<String, Object> row =
                    rows.isEmpty()
                            ? Map.of()
                            : rows.get(0);

            facts.put(
                    source.getSourceName(),
                    mapRow(source, row)
            );

            return;
        }

        facts.put(
                source.getSourceName(),
                rows.stream()
                        .map(row -> mapRow(source, row))
                        .toList()
        );
    }

    private Map<String, Object> mapRow(
            RuleDataSource source,
            Map<String, Object> row
    ) {
        Map<String, Object> mapped = new LinkedHashMap<>();

        if (source.getColumns() == null || source.getColumns().isEmpty()) {
            mapped.putAll(row);
            return mapped;
        }

        source.getColumns().stream()
                .filter(column -> column.getDeletedAt() == null)
                .sorted((a, b) -> Integer.compare(a.getOrderNo(), b.getOrderNo()))
                .forEach(column ->
                        putColumn(
                                mapped,
                                row,
                                column
                        )
                );

        return mapped;
    }

    private void putColumn(
            Map<String, Object> mapped,
            Map<String, Object> row,
            RuleColumnMapping column
    ) {
        Object value = row.get(column.getColumnName());

        if (column.isRequiredFlag() && value == null) {
            throw new RuntimeException(
                    "Rule Fact に必要な値が取得できません。 columnName="
                            + column.getColumnName()
                            + ", factKey="
                            + column.getFactKey()
            );
        }

        mapped.put(
                column.getFactKey(),
                value
        );
    }

    private Map<String, Object> toParameterMap(
            RuleContextRequest request
    ) {
        if (request == null || request.parameters() == null) {
            return Map.of();
        }

        return request.parameters();
    }
}