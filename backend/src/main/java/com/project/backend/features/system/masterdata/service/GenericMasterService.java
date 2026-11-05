package com.project.backend.features.system.masterdata.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.masterdata.contract.GenericMasterContractRegistry;
import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;
import com.project.backend.features.system.masterdata.contract.GenericMasterSynchronizerRegistry;
import com.project.backend.features.system.masterdata.dto.GenericMasterDefinitionResponse;
import com.project.backend.features.system.masterdata.dto.GenericMasterDefinitionSaveRequest;
import com.project.backend.features.system.masterdata.dto.GenericMasterFieldRequest;
import com.project.backend.features.system.masterdata.dto.GenericMasterFieldResponse;
import com.project.backend.features.system.masterdata.dto.GenericMasterRecordResponse;
import com.project.backend.features.system.masterdata.dto.GenericMasterRecordSaveRequest;
import com.project.backend.features.system.masterdata.entity.GenericMasterDefinition;
import com.project.backend.features.system.masterdata.entity.GenericMasterFieldDefinition;
import com.project.backend.features.system.masterdata.entity.GenericMasterFieldType;
import com.project.backend.features.system.masterdata.entity.GenericMasterPlacement;
import com.project.backend.features.system.masterdata.entity.GenericMasterRecord;
import com.project.backend.features.system.masterdata.entity.GenericMasterStatus;
import com.project.backend.features.system.masterdata.repository.GenericMasterDefinitionRepository;
import com.project.backend.features.system.masterdata.repository.GenericMasterFieldDefinitionRepository;
import com.project.backend.features.system.masterdata.repository.GenericMasterRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GenericMasterService {

    private static final TypeReference<Map<String, Object>> VALUE_MAP_TYPE =
            new TypeReference<>() { };

    private final GenericMasterDefinitionRepository definitionRepository;
    private final GenericMasterFieldDefinitionRepository fieldRepository;
    private final GenericMasterRecordRepository recordRepository;
    private final GenericMasterContractRegistry contractRegistry;
    private final GenericMasterSynchronizerRegistry synchronizerRegistry;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<GenericMasterDefinitionResponse> findDefinitions() {
        String tenantId = TenantContext.getTenantId();
        return definitionRepository
                .findByTenantIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(tenantId)
                .stream()
                .map(definition -> toDefinitionResponse(definition, tenantId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GenericMasterDefinitionResponse> findPublishedDefinitions(
            GenericMasterPlacement placement
    ) {
        String tenantId = TenantContext.getTenantId();
        return definitionRepository
                .findByTenantIdAndPlacementAndStatusAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        tenantId,
                        placement,
                        GenericMasterStatus.PUBLISHED
                )
                .stream()
                .map(definition -> toDefinitionResponse(definition, tenantId))
                .toList();
    }

    public GenericMasterDefinitionResponse createDefinition(
            GenericMasterDefinitionSaveRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        if (definitionRepository.existsByTenantIdAndMasterCodeAndDeletedAtIsNull(
                tenantId, request.masterCode()
        )) {
            throw new IllegalArgumentException("同じマスターコードが既に登録されています。");
        }
        validateFields(request.fields());
        GenericMasterDefinition definition = new GenericMasterDefinition();
        definition.setMasterCode(request.masterCode());
        definition.setStatus(GenericMasterStatus.DRAFT);
        applyDefinition(definition, request);
        definition = definitionRepository.save(definition);
        replaceFields(definition, request.fields(), tenantId);
        return toDefinitionResponse(definition, tenantId);
    }

    public GenericMasterDefinitionResponse updateDefinition(
            Long id,
            GenericMasterDefinitionSaveRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        GenericMasterDefinition definition = requireDefinition(id, tenantId);
        if (!definition.getMasterCode().equals(request.masterCode())) {
            throw new IllegalArgumentException("公開契約を保護するためマスターコードは変更できません。");
        }
        validateFields(request.fields());
        if (definition.getStatus() == GenericMasterStatus.PUBLISHED
                && recordRepository.existsByTenantIdAndDefinitionIdAndDeletedAtIsNull(
                        tenantId, id
                )) {
            validatePublishedStructure(id, request.fields(), tenantId);
        }
        applyDefinition(definition, request);
        replaceFields(definition, request.fields(), tenantId);
        return toDefinitionResponse(definition, tenantId);
    }

    public GenericMasterDefinitionResponse publish(Long id) {
        String tenantId = TenantContext.getTenantId();
        GenericMasterDefinition definition = requireDefinition(id, tenantId);
        List<GenericMasterFieldDefinition> fields = fields(id, tenantId);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("項目が1件以上必要です。");
        }
        definition.setStatus(GenericMasterStatus.PUBLISHED);
        definition.setActiveFlag(true);
        return toDefinitionResponse(definition, tenantId);
    }

    public void deleteDefinition(Long id) {
        String tenantId = TenantContext.getTenantId();
        GenericMasterDefinition definition = requireDefinition(id, tenantId);
        if (recordRepository.existsByTenantIdAndDefinitionIdAndDeletedAtIsNull(
                tenantId, id
        )) {
            throw new IllegalArgumentException(
                    "登録済みデータがあるため削除できません。無効化してください。"
            );
        }
        Instant now = Instant.now(clock);
        definition.setDeletedAt(now);
        fields(id, tenantId).forEach(field -> field.setDeletedAt(now));
    }

    @Transactional(readOnly = true)
    public List<GenericMasterRecordResponse> findRecords(Long definitionId) {
        String tenantId = TenantContext.getTenantId();
        requirePublishedDefinition(definitionId, tenantId);
        return recordRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByIdAsc(
                        tenantId, definitionId
                )
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    public GenericMasterRecordResponse createRecord(
            Long definitionId,
            GenericMasterRecordSaveRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        GenericMasterDefinition definition = requirePublishedDefinition(definitionId, tenantId);
        Map<String, Object> normalized = validateAndNormalizeValues(
                fields(definitionId, tenantId), request.values()
        );
        validateBusinessContract(
                definition,
                null,
                normalized,
                request.activeFlag(),
                tenantId
        );
        GenericMasterRecord record = new GenericMasterRecord();
        record.setDefinitionId(definitionId);
        record.setValuesJson(writeJson(normalized));
        record.setActiveFlag(request.activeFlag());
        GenericMasterRecord saved = recordRepository.save(record);
        synchronizeBusinessProjection(definition, tenantId);
        return toRecordResponse(saved);
    }

    public GenericMasterRecordResponse updateRecord(
            Long definitionId,
            Long recordId,
            GenericMasterRecordSaveRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        GenericMasterDefinition definition = requirePublishedDefinition(definitionId, tenantId);
        GenericMasterRecord record = recordRepository
                .findByIdAndTenantIdAndDefinitionIdAndDeletedAtIsNull(
                        recordId, tenantId, definitionId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "汎用マスターデータが見つかりません。id=" + recordId
                ));
        if (request.lockVersion() != null
                && request.lockVersion() != record.getLockVersion()) {
            throw new IllegalArgumentException(
                    "他の利用者が更新しています。再読み込みしてください。"
            );
        }
        Map<String, Object> normalized = validateAndNormalizeValues(
                fields(definitionId, tenantId), request.values()
        );
        validateBusinessContract(
                definition,
                recordId,
                normalized,
                request.activeFlag(),
                tenantId
        );
        record.setValuesJson(writeJson(normalized));
        record.setActiveFlag(request.activeFlag());
        synchronizeBusinessProjection(definition, tenantId);
        return toRecordResponse(record);
    }

    public void deleteRecord(Long definitionId, Long recordId) {
        String tenantId = TenantContext.getTenantId();
        GenericMasterDefinition definition = requirePublishedDefinition(definitionId, tenantId);
        GenericMasterRecord record = recordRepository
                .findByIdAndTenantIdAndDefinitionIdAndDeletedAtIsNull(
                        recordId, tenantId, definitionId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "汎用マスターデータが見つかりません。id=" + recordId
                ));
        List<GenericMasterRecordCandidate> remaining = candidates(
                definitionId, tenantId, recordId
        );
        contractRegistry.validate(definition.getMasterCode(), remaining);
        record.setDeletedAt(Instant.now(clock));
        synchronizerRegistry.synchronize(definition.getMasterCode(), remaining);
    }

    private void applyDefinition(
            GenericMasterDefinition definition,
            GenericMasterDefinitionSaveRequest request
    ) {
        definition.setMasterName(request.masterName());
        definition.setTabName(request.tabName());
        definition.setPlacement(request.placement());
        definition.setDescription(request.description());
        definition.setDisplayOrder(request.displayOrder());
        definition.setActiveFlag(request.activeFlag());
    }

    private void replaceFields(
            GenericMasterDefinition definition,
            List<GenericMasterFieldRequest> requests,
            String tenantId
    ) {
        Map<String, GenericMasterFieldDefinition> existing = new HashMap<>();
        fieldRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        tenantId, definition.getId()
                )
                .forEach(field -> existing.put(field.getFieldKey(), field));
        Set<String> requestedKeys = new HashSet<>();
        for (GenericMasterFieldRequest request : requests) {
            requestedKeys.add(request.fieldKey());
            GenericMasterFieldDefinition field = existing.getOrDefault(
                    request.fieldKey(), new GenericMasterFieldDefinition()
            );
            field.setDefinitionId(definition.getId());
            field.setFieldKey(request.fieldKey());
            field.setFieldLabel(request.fieldLabel());
            field.setFieldType(request.fieldType());
            field.setRequiredFlag(request.requiredFlag());
            field.setListVisibleFlag(request.listVisibleFlag());
            field.setSearchableFlag(request.searchableFlag());
            field.setDisplayOrder(request.displayOrder());
            field.setUnit(request.unit());
            field.setPlaceholder(request.placeholder());
            field.setValidationPattern(request.validationPattern());
            field.setOptionsJson(request.optionsJson());
            field.setDeletedAt(null);
            fieldRepository.save(field);
        }
        Instant now = Instant.now(clock);
        existing.values().stream()
                .filter(field -> !requestedKeys.contains(field.getFieldKey()))
                .forEach(field -> field.setDeletedAt(now));
    }

    private void validateFields(List<GenericMasterFieldRequest> fields) {
        Set<String> keys = new HashSet<>();
        for (GenericMasterFieldRequest field : fields) {
            if (!keys.add(field.fieldKey())) {
                throw new IllegalArgumentException("項目キーが重複しています: " + field.fieldKey());
            }
            if (StringUtils.hasText(field.validationPattern())) {
                try {
                    Pattern.compile(field.validationPattern());
                } catch (PatternSyntaxException exception) {
                    throw new IllegalArgumentException(
                            field.fieldLabel() + "の入力規則が不正です。"
                    );
                }
            }
            if (field.fieldType() == GenericMasterFieldType.SELECT) {
                validateOptions(field);
            }
        }
    }

    private void validateOptions(GenericMasterFieldRequest field) {
        try {
            JsonNode node = objectMapper.readTree(field.optionsJson());
            if (!node.isArray() || node.isEmpty()) {
                throw new IllegalArgumentException(
                        field.fieldLabel() + "の選択肢をJSON配列で指定してください。"
                );
            }
        } catch (JsonProcessingException | NullPointerException exception) {
            throw new IllegalArgumentException(
                    field.fieldLabel() + "の選択肢をJSON配列で指定してください。"
            );
        }
    }

    private void validatePublishedStructure(
            Long definitionId,
            List<GenericMasterFieldRequest> requested,
            String tenantId
    ) {
        Map<String, GenericMasterFieldType> existing = new HashMap<>();
        fields(definitionId, tenantId).forEach(field ->
                existing.put(field.getFieldKey(), field.getFieldType())
        );
        boolean changed = requested.size() != existing.size()
                || requested.stream().anyMatch(field ->
                        existing.get(field.fieldKey()) != field.fieldType()
                );
        if (changed) {
            throw new IllegalArgumentException(
                    "データ登録済みの公開マスターでは項目キー・型を変更できません。"
            );
        }
    }

    private Map<String, Object> validateAndNormalizeValues(
            List<GenericMasterFieldDefinition> fields,
            Map<String, Object> values
    ) {
        Set<String> knownKeys = new HashSet<>();
        Map<String, Object> normalized = new HashMap<>();
        for (GenericMasterFieldDefinition field : fields) {
            knownKeys.add(field.getFieldKey());
            Object value = values.get(field.getFieldKey());
            if (field.isRequiredFlag() && isBlank(value)) {
                throw new IllegalArgumentException(field.getFieldLabel() + "は必須です。");
            }
            if (!isBlank(value)) {
                validateValue(field, value);
            }
            normalized.put(field.getFieldKey(), value);
        }
        if (values.keySet().stream().anyMatch(key -> !knownKeys.contains(key))) {
            throw new IllegalArgumentException("定義されていない項目が含まれています。");
        }
        return normalized;
    }

    private void validateValue(GenericMasterFieldDefinition field, Object value) {
        String text = String.valueOf(value);
        try {
            switch (field.getFieldType()) {
                case NUMBER -> new BigDecimal(text);
                case DATE -> LocalDate.parse(text);
                case BOOLEAN -> {
                    if (!(value instanceof Boolean)
                            && !"true".equalsIgnoreCase(text)
                            && !"false".equalsIgnoreCase(text)) {
                        throw new IllegalArgumentException();
                    }
                }
                case SELECT -> validateSelectValue(field, text);
                default -> { }
            }
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new IllegalArgumentException(field.getFieldLabel() + "の形式が不正です。");
        }
        if (StringUtils.hasText(field.getValidationPattern())
                && !Pattern.matches(field.getValidationPattern(), text)) {
            throw new IllegalArgumentException(field.getFieldLabel() + "の入力規則に一致しません。");
        }
    }

    private void validateSelectValue(GenericMasterFieldDefinition field, String value) {
        try {
            JsonNode options = objectMapper.readTree(field.getOptionsJson());
            boolean exists = false;
            for (JsonNode option : options) {
                String optionValue = option.isObject()
                        ? option.path("value").asText()
                        : option.asText();
                if (optionValue.equals(value)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                throw new IllegalArgumentException(
                        field.getFieldLabel() + "の選択値が不正です。"
                );
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("選択肢定義が不正です。", exception);
        }
    }

    private boolean isBlank(Object value) {
        return value == null || (value instanceof String string && !StringUtils.hasText(string));
    }

    private void validateBusinessContract(
            GenericMasterDefinition definition,
            Long editingRecordId,
            Map<String, Object> values,
            boolean activeFlag,
            String tenantId
    ) {
        List<GenericMasterRecordCandidate> candidates = candidates(
                definition.getId(), tenantId, editingRecordId
        );
        candidates.add(new GenericMasterRecordCandidate(
                editingRecordId, values, activeFlag
        ));
        contractRegistry.validate(definition.getMasterCode(), candidates);
    }

    private List<GenericMasterRecordCandidate> candidates(
            Long definitionId,
            String tenantId,
            Long excludedRecordId
    ) {
        return recordRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByIdAsc(
                        tenantId, definitionId
                )
                .stream()
                .filter(record -> excludedRecordId == null
                        || !excludedRecordId.equals(record.getId()))
                .map(record -> new GenericMasterRecordCandidate(
                        record.getId(), readValues(record), record.isActiveFlag()
                ))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    private void synchronizeBusinessProjection(
            GenericMasterDefinition definition,
            String tenantId
    ) {
        synchronizerRegistry.synchronize(
                definition.getMasterCode(),
                candidates(definition.getId(), tenantId, null)
        );
    }

    private Map<String, Object> readValues(GenericMasterRecord record) {
        try {
            return objectMapper.readValue(record.getValuesJson(), VALUE_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "汎用マスターデータの読み込みに失敗しました。id=" + record.getId(),
                    exception
            );
        }
    }

    private GenericMasterDefinition requireDefinition(Long id, String tenantId) {
        return definitionRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "汎用マスター定義が見つかりません。id=" + id
                ));
    }

    private GenericMasterDefinition requirePublishedDefinition(Long id, String tenantId) {
        GenericMasterDefinition definition = requireDefinition(id, tenantId);
        if (definition.getStatus() != GenericMasterStatus.PUBLISHED
                || !definition.isActiveFlag()) {
            throw new IllegalArgumentException("公開中の汎用マスターではありません。");
        }
        return definition;
    }

    private List<GenericMasterFieldDefinition> fields(Long definitionId, String tenantId) {
        return fieldRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        tenantId, definitionId
                );
    }

    private GenericMasterDefinitionResponse toDefinitionResponse(
            GenericMasterDefinition definition,
            String tenantId
    ) {
        List<GenericMasterFieldResponse> responses = fields(definition.getId(), tenantId)
                .stream()
                .map(this::toFieldResponse)
                .toList();
        return new GenericMasterDefinitionResponse(
                definition.getId(),
                definition.getMasterCode(),
                definition.getMasterName(),
                definition.getTabName(),
                definition.getPlacement(),
                definition.getDescription(),
                definition.getDisplayOrder(),
                definition.getStatus(),
                definition.isActiveFlag(),
                responses
        );
    }

    private GenericMasterFieldResponse toFieldResponse(GenericMasterFieldDefinition field) {
        return new GenericMasterFieldResponse(
                field.getId(),
                field.getFieldKey(),
                field.getFieldLabel(),
                field.getFieldType(),
                field.isRequiredFlag(),
                field.isListVisibleFlag(),
                field.isSearchableFlag(),
                field.getDisplayOrder(),
                field.getUnit(),
                field.getPlaceholder(),
                field.getValidationPattern(),
                field.getOptionsJson()
        );
    }

    private GenericMasterRecordResponse toRecordResponse(GenericMasterRecord record) {
        return new GenericMasterRecordResponse(
                record.getId(),
                record.getDefinitionId(),
                readValues(record),
                record.isActiveFlag(),
                record.getLockVersion(),
                record.getUpdatedAt()
        );
    }

    private String writeJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("汎用マスターデータをJSONへ変換できません。", exception);
        }
    }
}
