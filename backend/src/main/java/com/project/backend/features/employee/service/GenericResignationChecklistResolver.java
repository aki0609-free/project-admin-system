package com.project.backend.features.employee.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.masterdata.entity.GenericMasterDefinition;
import com.project.backend.features.system.masterdata.entity.GenericMasterRecord;
import com.project.backend.features.system.masterdata.entity.GenericMasterStatus;
import com.project.backend.features.system.masterdata.repository.GenericMasterDefinitionRepository;
import com.project.backend.features.system.masterdata.repository.GenericMasterRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenericResignationChecklistResolver {

    private static final TypeReference<Map<String, Object>> VALUE_MAP_TYPE =
            new TypeReference<>() { };

    private final GenericMasterDefinitionRepository definitionRepository;
    private final GenericMasterRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public Resolution resolve() {
        String tenantId = TenantContext.getTenantId();
        Optional<GenericMasterDefinition> definition = definitionRepository
                .findByTenantIdAndMasterCodeAndStatusAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId,
                        ResignationChecklistGenericMasterContract.MASTER_CODE,
                        GenericMasterStatus.PUBLISHED
                );
        if (definition.isEmpty()) {
            return new Resolution(false, List.of());
        }
        List<ChecklistItem> items = recordRepository
                .findByTenantIdAndDefinitionIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        tenantId, definition.get().getId()
                )
                .stream()
                .map(this::toItem)
                .sorted(Comparator.comparingInt(ChecklistItem::displayOrder)
                        .thenComparing(ChecklistItem::id))
                .toList();
        return new Resolution(true, items);
    }

    private ChecklistItem toItem(GenericMasterRecord record) {
        Map<String, Object> values = readValues(record);
        return new ChecklistItem(
                record.getId(),
                required(values, "CODE"),
                required(values, "NAME"),
                nullable(values, "DESCRIPTION"),
                booleanValue(values, "REQUIRED_FLAG"),
                new BigDecimal(required(values, "DISPLAY_ORDER")).intValueExact()
        );
    }

    private Map<String, Object> readValues(GenericMasterRecord record) {
        try {
            return objectMapper.readValue(record.getValuesJson(), VALUE_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "退職時TODOを読み込めません。id=" + record.getId(),
                    exception
            );
        }
    }

    private String required(Map<String, Object> values, String key) {
        String value = nullable(values, key);
        if (value == null) {
            throw new IllegalStateException("退職時TODOの必須項目がありません: " + key);
        }
        return value;
    }

    private String nullable(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null || String.valueOf(value).isBlank()
                ? null
                : String.valueOf(value);
    }

    private boolean booleanValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Boolean booleanValue
                ? booleanValue
                : Boolean.parseBoolean(required(values, key));
    }

    public record Resolution(boolean configured, List<ChecklistItem> items) {
    }

    public record ChecklistItem(
            Long id,
            String code,
            String name,
            String description,
            boolean requiredFlag,
            int displayOrder
    ) {
    }
}
