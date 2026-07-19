package com.project.backend.features.system.rule.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.dto.*;
import com.project.backend.features.system.rule.entity.*;
import com.project.backend.features.system.rule.enums.RuleDataType;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.enums.RuleType;

@Component
public class RuleMasterMapper {

    public RuleMasterResponse toResponse(RuleMaster entity) {
        return RuleMasterResponse.builder()
                .id(entity.getId())
                .ruleName(entity.getRuleName())
                .ruleDisplayName(entity.getRuleDisplayName())
                .ruleType(entity.getRuleType())
                .dslType(entity.getDslType())
                .dslText(entity.getDslText())
                .ruleBeanName(entity.getRuleBeanName())
                .resultFactKey(entity.getResultFactKey())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .activeFlag(entity.isActiveFlag())
                .parameters(toParameterResponses(entity.getParameters()))
                .dataSources(toDataSourceResponses(entity.getDataSources()))
                .build();
    }

    public List<RuleMasterResponse> toResponseList(List<RuleMaster> entities) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public void applyRequest(
            RuleMaster entity,
            RuleMasterSaveRequest request
    ) {
        entity.setRuleName(request.ruleName());
        entity.setRuleDisplayName(request.ruleDisplayName());
        entity.setRuleType(request.ruleType() != null ? request.ruleType() : RuleType.GENERAL);
        entity.setDslType(request.dslType() != null ? request.dslType() : RuleDslType.MVEL);
        entity.setDslText(request.dslText());
        entity.setRuleBeanName(request.ruleBeanName());
        entity.setResultFactKey(StringUtils.hasText(request.resultFactKey()) ? request.resultFactKey() : "result");
        entity.setDescription(request.description());
        entity.setPriority(request.priority() > 0 ? request.priority() : 100);
        entity.setActiveFlag(request.activeFlag());

        entity.clearParameters();
        safeParameters(request).stream()
                .sorted(Comparator.comparingInt(RuleParameterSaveRequest::orderNo))
                .forEach(item -> entity.addParameter(toParameter(item)));

        entity.clearDataSources();
        safeDataSources(request).stream()
                .sorted(Comparator.comparingInt(RuleDataSourceSaveRequest::orderNo))
                .forEach(item -> entity.addDataSource(toDataSource(item)));
    }

    private RuleParameter toParameter(RuleParameterSaveRequest request) {
        RuleParameter entity = new RuleParameter();

        entity.setParamName(request.paramName());
        entity.setDataType(request.dataType() != null ? request.dataType() : RuleDataType.STRING);
        entity.setRequiredFlag(request.requiredFlag());
        entity.setDefaultValue(request.defaultValue());
        entity.setDescription(request.description());
        entity.setOrderNo(request.orderNo() > 0 ? request.orderNo() : 1);

        return entity;
    }

    private RuleDataSource toDataSource(RuleDataSourceSaveRequest request) {
        RuleDataSource entity = new RuleDataSource();

        entity.setSourceName(request.sourceName());
        entity.setTableName(request.tableName());
        entity.setWhereClause(request.whereClause());
        entity.setSingleRowFlag(request.singleRowFlag());
        entity.setActiveFlag(request.activeFlag());
        entity.setOrderNo(request.orderNo() > 0 ? request.orderNo() : 1);

        entity.clearColumns();
        safeColumns(request).stream()
                .sorted(Comparator.comparingInt(RuleColumnMappingSaveRequest::orderNo))
                .forEach(item -> entity.addColumn(toColumnMapping(item)));

        return entity;
    }

    private RuleColumnMapping toColumnMapping(RuleColumnMappingSaveRequest request) {
        RuleColumnMapping entity = new RuleColumnMapping();

        entity.setColumnName(request.columnName());
        entity.setFactKey(request.factKey());
        entity.setDataType(request.dataType() != null ? request.dataType() : RuleDataType.STRING);
        entity.setRequiredFlag(request.requiredFlag());
        entity.setOrderNo(request.orderNo() > 0 ? request.orderNo() : 1);

        return entity;
    }

    private List<RuleParameterResponse> toParameterResponses(List<RuleParameter> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .filter(item -> item.getDeletedAt() == null)
                .sorted(Comparator.comparingInt(RuleParameter::getOrderNo))
                .map(item -> RuleParameterResponse.builder()
                        .id(item.getId())
                        .paramName(item.getParamName())
                        .dataType(item.getDataType())
                        .requiredFlag(item.isRequiredFlag())
                        .defaultValue(item.getDefaultValue())
                        .description(item.getDescription())
                        .orderNo(item.getOrderNo())
                        .build())
                .toList();
    }

    private List<RuleDataSourceResponse> toDataSourceResponses(List<RuleDataSource> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .filter(item -> item.getDeletedAt() == null)
                .sorted(Comparator.comparingInt(RuleDataSource::getOrderNo))
                .map(item -> RuleDataSourceResponse.builder()
                        .id(item.getId())
                        .sourceName(item.getSourceName())
                        .tableName(item.getTableName())
                        .whereClause(item.getWhereClause())
                        .singleRowFlag(item.isSingleRowFlag())
                        .activeFlag(item.isActiveFlag())
                        .orderNo(item.getOrderNo())
                        .columns(toColumnResponses(item.getColumns()))
                        .build())
                .toList();
    }

    private List<RuleColumnMappingResponse> toColumnResponses(List<RuleColumnMapping> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .filter(item -> item.getDeletedAt() == null)
                .sorted(Comparator.comparingInt(RuleColumnMapping::getOrderNo))
                .map(item -> RuleColumnMappingResponse.builder()
                        .id(item.getId())
                        .columnName(item.getColumnName())
                        .factKey(item.getFactKey())
                        .dataType(item.getDataType())
                        .requiredFlag(item.isRequiredFlag())
                        .orderNo(item.getOrderNo())
                        .build())
                .toList();
    }

    private List<RuleParameterSaveRequest> safeParameters(RuleMasterSaveRequest request) {
        return request.parameters() != null ? request.parameters() : List.of();
    }

    private List<RuleDataSourceSaveRequest> safeDataSources(RuleMasterSaveRequest request) {
        return request.dataSources() != null ? request.dataSources() : List.of();
    }

    private List<RuleColumnMappingSaveRequest> safeColumns(RuleDataSourceSaveRequest request) {
        return request.columns() != null ? request.columns() : List.of();
    }
}