package com.project.backend.features.system.settings.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.settings.dto.SystemSettingItemResponse;
import com.project.backend.features.system.settings.dto.SystemSettingValueSaveItem;
import com.project.backend.features.system.settings.dto.SystemSettingValueSaveRequest;
import com.project.backend.features.system.settings.entity.SystemSettingDefinition;
import com.project.backend.features.system.settings.entity.SystemSettingScope;
import com.project.backend.features.system.settings.entity.SystemSettingValue;
import com.project.backend.features.system.settings.entity.SystemSettingValueType;
import com.project.backend.features.system.settings.repository.SystemSettingDefinitionRepository;
import com.project.backend.features.system.settings.repository.SystemSettingValueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingService {

    private final SystemSettingDefinitionRepository definitionRepository;
    private final SystemSettingValueRepository valueRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SystemSettingItemResponse> findAll() {
        List<SystemSettingDefinition> definitions =
                definitionRepository.findEffective(currentTenantId());
        Map<Long, SystemSettingValue> values = valueRepository
                .findByTenantIdAndDefinitionIdInAndDeletedAtIsNull(
                        currentTenantId(),
                        definitions.stream().map(SystemSettingDefinition::getId).toList()
                )
                .stream()
                .collect(Collectors.toMap(value -> value.getDefinition().getId(), Function.identity()));
        return definitions.stream()
                .map(definition -> toResponse(definition, values.get(definition.getId())))
                .toList();
    }

    public List<SystemSettingItemResponse> save(SystemSettingValueSaveRequest request) {
        String tenantId = currentTenantId();
        Map<Long, SystemSettingDefinition> definitions = definitionRepository
                .findAllById(request.values().stream().map(SystemSettingValueSaveItem::definitionId).toList())
                .stream()
                .filter(SystemSettingDefinition::isActiveFlag)
                .filter(definition -> isEffectiveForTenant(definition, tenantId))
                .collect(Collectors.toMap(SystemSettingDefinition::getId, Function.identity()));
        if (definitions.size() != request.values().size()) {
            throw new IllegalArgumentException("存在しない、または無効な共通設定が含まれています。");
        }

        for (SystemSettingValueSaveItem item : request.values()) {
            SystemSettingDefinition definition = definitions.get(item.definitionId());
            saveValueForDefinition(definition, item.value());
        }
        return findAll();
    }

    public void saveValueForDefinition(SystemSettingDefinition definition, String value) {
        String tenantId = currentTenantId();
        if (!isEffectiveForTenant(definition, tenantId)) {
            throw new IllegalArgumentException("存在しない、または無効な共通設定です。");
        }
        String normalized = validateAndNormalize(definition, value);
        SystemSettingValue settingValue = valueRepository
                .findByTenantIdAndDefinitionId(tenantId, definition.getId())
                .orElseGet(SystemSettingValue::new);
        settingValue.setDefinition(definition);
        settingValue.setSettingValue(normalized);
        settingValue.setDeletedAt(null);
        valueRepository.save(settingValue);
    }

    @Transactional(readOnly = true)
    public String findValue(String largeKey, String middleKey, String smallKey) {
        return definitionRepository
                .findByLargeCategoryKeyAndMiddleCategoryKeyAndSmallCategoryKeyAndScopeAndActiveFlagTrueAndDeletedAtIsNull(
                        largeKey, middleKey, smallKey, SystemSettingScope.SYSTEM
                )
                .flatMap(definition -> valueRepository
                        .findByTenantIdAndDefinitionIdAndDeletedAtIsNull(
                                currentTenantId(), definition.getId()
                        ))
                .map(SystemSettingValue::getSettingValue)
                .orElse("");
    }

    public void saveValue(String largeKey, String middleKey, String smallKey, String value) {
        SystemSettingDefinition definition = definitionRepository
                .findByLargeCategoryKeyAndMiddleCategoryKeyAndSmallCategoryKeyAndScopeAndActiveFlagTrueAndDeletedAtIsNull(
                        largeKey, middleKey, smallKey, SystemSettingScope.SYSTEM
                )
                .orElseThrow(() -> new IllegalStateException("共通設定マスターが登録されていません。"));
        save(new SystemSettingValueSaveRequest(List.of(
                new SystemSettingValueSaveItem(definition.getId(), value)
        )));
    }

    private String validateAndNormalize(SystemSettingDefinition definition, String value) {
        return validateValue(
                definition.getSettingName(),
                definition.getValueType(),
                definition.isRequiredFlag(),
                definition.getValidationPattern(),
                value
        );
    }

    public String validateValue(
            SystemSettingDefinition definition,
            String value,
            SystemSettingValueType valueType,
            boolean required,
            String validationPattern
    ) {
        return validateValue(
                definition.getSettingName(),
                valueType,
                required,
                validationPattern,
                value
        );
    }

    private String validateValue(
            String settingName,
            SystemSettingValueType valueType,
            boolean required,
            String validationPattern,
            String value
    ) {
        String normalized = value == null ? "" : value.trim();
        if (required && normalized.isEmpty()) {
            throw new IllegalArgumentException(settingName + "は必須です。");
        }
        if (normalized.length() > 10000) {
            throw new IllegalArgumentException(settingName + "が長すぎます。");
        }
        if (!normalized.isEmpty()) {
            try {
                switch (valueType) {
                    case URL -> normalized = normalizeHttpsUrl(normalized);
                    case NUMBER -> new BigDecimal(normalized);
                    case BOOLEAN -> {
                        if (!"true".equalsIgnoreCase(normalized) && !"false".equalsIgnoreCase(normalized)) {
                            throw new IllegalArgumentException();
                        }
                        normalized = normalized.toLowerCase();
                    }
                    case DATE -> LocalDate.parse(normalized);
                    case JSON -> objectMapper.readTree(normalized);
                    case STRING -> { }
                }
            } catch (Exception exception) {
                throw new IllegalArgumentException(
                        settingName + "の形式が正しくありません。"
                );
            }
            if (validationPattern != null
                    && !validationPattern.isBlank()
                    && !Pattern.matches(validationPattern, normalized)) {
                throw new IllegalArgumentException(settingName + "の形式が正しくありません。");
            }
        }
        return normalized;
    }

    private String normalizeHttpsUrl(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException();
        }
        return uri.toASCIIString();
    }

    private boolean isEffectiveForTenant(SystemSettingDefinition definition, String tenantId) {
        return definition.getDeletedAt() == null
                && (definition.getScope() == SystemSettingScope.SYSTEM
                || (definition.getScope() == SystemSettingScope.TENANT
                && tenantId.equals(definition.getOwnerTenantId())));
    }

    private SystemSettingItemResponse toResponse(
            SystemSettingDefinition definition,
            SystemSettingValue value
    ) {
        return new SystemSettingItemResponse(
                definition.getId(),
                definition.getLargeCategoryKey(),
                definition.getLargeCategoryName(),
                definition.getMiddleCategoryKey(),
                definition.getMiddleCategoryName(),
                definition.getSmallCategoryKey(),
                definition.getSettingName(),
                definition.getDescription(),
                definition.getValueType().name(),
                definition.isRequiredFlag(),
                definition.getPlaceholder(),
                definition.getDisplayOrder(),
                value == null ? "" : value.getSettingValue()
        );
    }

    private String currentTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("TenantContext に tenantId が設定されていません。");
        }
        return tenantId;
    }
}
