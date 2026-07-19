package com.project.backend.features.system.report.service.loader;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.service.api.OutputTableQueryService;
import com.project.backend.features.system.report.service.resolver.ReportHeaderResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportOutputRowLoader {

    private final OutputTableQueryService outputTableQueryService;
    private final ReportHeaderResolver reportHeaderResolver;

    public List<Map<String, Object>> loadRows(
            ReportMaster reportMaster,
            String executionId
    ) {
        if (StringUtils.hasText(reportMaster.getQuerySql())) {
            return outputTableQueryService.query(
                    reportMaster.getQuerySql(),
                    Map.of("executionId", executionId)
            );
        }

        return outputTableQueryService.findByExecutionId(
                reportMaster.resolveOutputTableName(),
                executionId
        );
    }

    public List<String> extractHeaders(List<Map<String, Object>> rows) {
        return reportHeaderResolver.resolve(rows);
    }
}