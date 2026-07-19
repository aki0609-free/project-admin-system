package com.project.backend.features.system.report.service.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportParam;
import com.project.backend.features.system.report.service.converter.ReportParamValueConverter;
import com.project.backend.common.util.ApplicationCollectionUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportInputBindBuilder {

    private final ReportParamValueConverter reportParamValueConverter;

    public List<Map<String, Object>> buildRows(
            ReportMaster reportMaster,
            String executionId,
            Map<String, Object> requestParams,
            List<ReportParam> paramDefinitions
    ) {
        ReportParam multipleParam = findMultipleParam(paramDefinitions);

        if (multipleParam == null) {
            return List.of(buildSingleRow(executionId, requestParams, paramDefinitions));
        }

        Object rawMultipleValue = resolveRawValue(multipleParam, requestParams);

        if (rawMultipleValue == null) {
            if (Boolean.TRUE.equals(multipleParam.getRequiredFlag())) {
                throw new RuntimeException("必須パラメータが不足しています: " + multipleParam.getParamName());
            }

            return List.of(buildSingleRow(executionId, requestParams, paramDefinitions));
        }

        List<?> values = ApplicationCollectionUtils.toList(rawMultipleValue);

        if (values.isEmpty()) {
            throw new RuntimeException("複数選択パラメータが空です: " + multipleParam.getParamName());
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        for (Object value : values) {
            Map<String, Object> row = buildSingleRow(executionId, requestParams, paramDefinitions);

            Object converted = reportParamValueConverter.convert(
                    value,
                    multipleParam.getParamType()
            );

            row.put(multipleParam.resolveInputColumnName(), converted);
            rows.add(row);
        }

        return rows;
    }

    private Map<String, Object> buildSingleRow(
            String executionId,
            Map<String, Object> requestParams,
            List<ReportParam> paramDefinitions
    ) {
        Map<String, Object> bind = new LinkedHashMap<>();
        bind.put("execution_id", executionId);

        for (ReportParam param : paramDefinitions) {
            if (!Boolean.TRUE.equals(param.getActiveFlag())) {
                continue;
            }

            if (Boolean.TRUE.equals(param.getMultipleFlag())) {
                continue;
            }

            Object rawValue = resolveRawValue(param, requestParams);

            if (rawValue == null) {
                if (Boolean.TRUE.equals(param.getRequiredFlag())) {
                    throw new RuntimeException("必須パラメータが不足しています: " + param.getParamName());
                }
                continue;
            }

            Object converted = reportParamValueConverter.convert(
                    rawValue,
                    param.getParamType()
            );

            bind.put(param.resolveInputColumnName(), converted);
        }

        return bind;
    }

    private ReportParam findMultipleParam(List<ReportParam> params) {
        List<ReportParam> multipleParams = params.stream()
                .filter(param -> Boolean.TRUE.equals(param.getActiveFlag()))
                .filter(param -> Boolean.TRUE.equals(param.getMultipleFlag()))
                .toList();

        if (multipleParams.size() > 1) {
            throw new RuntimeException("multipleFlag=true のパラメータは1帳票につき1つまでにしてください。");
        }

        return multipleParams.isEmpty() ? null : multipleParams.get(0);
    }

    private Object resolveRawValue(
            ReportParam param,
            Map<String, Object> requestParams
    ) {
        Object rawValue = requestParams != null
                ? requestParams.get(param.getParamName())
                : null;

        if (rawValue == null && param.getDefaultValue() != null) {
            rawValue = param.getDefaultValue();
        }

        return rawValue;
    }
}