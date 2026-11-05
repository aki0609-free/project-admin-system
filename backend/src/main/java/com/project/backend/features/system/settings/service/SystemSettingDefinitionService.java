package com.project.backend.features.system.settings.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.settings.dto.SystemSettingDefinitionCreateRequest;
import com.project.backend.features.system.settings.dto.SystemSettingDefinitionResponse;
import com.project.backend.features.system.settings.dto.SystemSettingDefinitionUpdateRequest;
import com.project.backend.features.system.settings.entity.SystemSettingDefinition;
import com.project.backend.features.system.settings.entity.SystemSettingScope;
import com.project.backend.features.system.settings.entity.SystemSettingValueType;
import com.project.backend.features.system.settings.repository.SystemSettingDefinitionRepository;
import com.project.backend.features.system.settings.repository.SystemSettingValueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingDefinitionService {

    private final SystemSettingDefinitionRepository definitionRepository;
    private final SystemSettingValueRepository valueRepository;
    private final SystemSettingService settingService;

    @Transactional(readOnly = true)
    public List<SystemSettingDefinitionResponse> findAll() {
        String tenantId = currentTenantId();
        return definitionRepository.findManageable(tenantId).stream()
                .map(definition -> toResponse(definition, tenantId))
                .toList();
    }

    public SystemSettingDefinitionResponse create(SystemSettingDefinitionCreateRequest request) {
        String tenantId = currentTenantId();
        if (definitionRepository.existsEffectiveKey(
                tenantId,
                request.largeCategoryKey(),
                request.middleCategoryKey(),
                request.smallCategoryKey()
        )) {
            throw new IllegalArgumentException("同じ分類キーの共通設定が既に登録されています。");
        }

        SystemSettingDefinition definition = definitionRepository
                .findByLargeCategoryKeyAndMiddleCategoryKeyAndSmallCategoryKeyAndScopeAndOwnerTenantId(
                        request.largeCategoryKey(),
                        request.middleCategoryKey(),
                        request.smallCategoryKey(),
                        SystemSettingScope.TENANT,
                        tenantId
                )
                .orElseGet(SystemSettingDefinition::new);
        definition.setScope(SystemSettingScope.TENANT);
        definition.setOwnerTenantId(tenantId);
        definition.setLargeCategoryKey(request.largeCategoryKey());
        definition.setMiddleCategoryKey(request.middleCategoryKey());
        definition.setSmallCategoryKey(request.smallCategoryKey());
        definition.setSystemManagedFlag(false);
        applyEditableFields(definition, request);
        definition.setDeletedAt(null);
        Instant now = Instant.now();
        if (definition.getCreatedAt() == null) {
            definition.setCreatedAt(now);
        }
        definition.setUpdatedAt(now);
        SystemSettingDefinition saved = definitionRepository.save(definition);
        settingService.saveValueForDefinition(saved, request.settingValue());
        return toResponse(saved, tenantId);
    }

    public SystemSettingDefinitionResponse update(
            Long id,
            SystemSettingDefinitionUpdateRequest request
    ) {
        String tenantId = currentTenantId();
        SystemSettingDefinition definition = findTenantDefinition(id, tenantId);
        SystemSettingValueType valueType = parseValueType(request.valueType());
        valueRepository.findByTenantIdAndDefinitionIdAndDeletedAtIsNull(tenantId, id)
                .ifPresent(value -> settingService.validateValue(
                        definition,
                        value.getSettingValue(),
                        valueType,
                        request.requiredFlag(),
                        request.validationPattern()
                ));
        applyEditableFields(definition, request, valueType);
        definition.setUpdatedAt(Instant.now());
        SystemSettingDefinition saved = definitionRepository.save(definition);
        settingService.saveValueForDefinition(saved, request.settingValue());
        return toResponse(saved, tenantId);
    }

    public void delete(Long id) {
        String tenantId = currentTenantId();
        SystemSettingDefinition definition = findTenantDefinition(id, tenantId);
        Instant now = Instant.now();
        definition.setActiveFlag(false);
        definition.setDeletedAt(now);
        definition.setUpdatedAt(now);
        definitionRepository.save(definition);
        valueRepository.findByTenantIdAndDefinitionIdAndDeletedAtIsNull(tenantId, id)
                .ifPresent(value -> {
                    value.setDeletedAt(now);
                    valueRepository.save(value);
                });
    }

    private SystemSettingDefinition findTenantDefinition(Long id, String tenantId) {
        return definitionRepository
                .findByIdAndScopeAndOwnerTenantIdAndDeletedAtIsNull(
                        id, SystemSettingScope.TENANT, tenantId
                )
                .filter(definition -> !definition.isSystemManagedFlag())
                .orElseThrow(() -> new IllegalArgumentException(
                        "編集可能な会社独自設定が見つかりません。"
                ));
    }

    private void applyEditableFields(
            SystemSettingDefinition definition,
            SystemSettingDefinitionCreateRequest request
    ) {
        applyEditableFields(
                definition,
                request.largeCategoryName(),
                request.middleCategoryName(),
                request.settingName(),
                request.description(),
                parseValueType(request.valueType()),
                request.requiredFlag(),
                request.validationPattern(),
                request.placeholder(),
                request.displayOrder(),
                request.sensitiveFlag(),
                request.activeFlag()
        );
    }

    private void applyEditableFields(
            SystemSettingDefinition definition,
            SystemSettingDefinitionUpdateRequest request,
            SystemSettingValueType valueType
    ) {
        applyEditableFields(
                definition,
                request.largeCategoryName(),
                request.middleCategoryName(),
                request.settingName(),
                request.description(),
                valueType,
                request.requiredFlag(),
                request.validationPattern(),
                request.placeholder(),
                request.displayOrder(),
                request.sensitiveFlag(),
                request.activeFlag()
        );
    }

    private void applyEditableFields(
            SystemSettingDefinition definition,
            String largeName,
            String middleName,
            String settingName,
            String description,
            SystemSettingValueType valueType,
            boolean required,
            String validationPattern,
            String placeholder,
            int displayOrder,
            boolean sensitive,
            boolean active
    ) {
        validatePattern(validationPattern);
        definition.setLargeCategoryName(largeName.trim());
        definition.setMiddleCategoryName(middleName.trim());
        definition.setSettingName(settingName.trim());
        definition.setDescription(trimToNull(description));
        definition.setValueType(valueType);
        definition.setRequiredFlag(required);
        definition.setValidationPattern(trimToNull(validationPattern));
        definition.setPlaceholder(trimToNull(placeholder));
        definition.setDisplayOrder(displayOrder);
        definition.setSensitiveFlag(sensitive);
        definition.setActiveFlag(active);
    }

    private void validatePattern(String validationPattern) {
        if (validationPattern == null || validationPattern.isBlank()) {
            return;
        }
        try {
            java.util.regex.Pattern.compile(validationPattern);
        } catch (java.util.regex.PatternSyntaxException exception) {
            throw new IllegalArgumentException("入力規則の正規表現が正しくありません。");
        }
    }

    private SystemSettingValueType parseValueType(String value) {
        try {
            return SystemSettingValueType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("未対応の値型です。");
        }
    }

    private SystemSettingDefinitionResponse toResponse(
            SystemSettingDefinition definition,
            String tenantId
    ) {
        boolean editable = definition.getScope() == SystemSettingScope.TENANT
                && tenantId.equals(definition.getOwnerTenantId())
                && !definition.isSystemManagedFlag();
        return new SystemSettingDefinitionResponse(
                definition.getId(),
                definition.getScope().name(),
                definition.getLargeCategoryKey(),
                definition.getLargeCategoryName(),
                definition.getMiddleCategoryKey(),
                definition.getMiddleCategoryName(),
                definition.getSmallCategoryKey(),
                definition.getSettingName(),
                definition.getDescription(),
                definition.getValueType().name(),
                definition.isRequiredFlag(),
                definition.getValidationPattern(),
                definition.getPlaceholder(),
                definition.getDisplayOrder(),
                definition.isSensitiveFlag(),
                definition.isActiveFlag(),
                definition.isSystemManagedFlag(),
                editable,
                valueRepository
                        .findByTenantIdAndDefinitionIdAndDeletedAtIsNull(
                                tenantId, definition.getId()
                        )
                        .map(value -> value.getSettingValue())
                        .orElse("")
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String currentTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("TenantContext に tenantId が設定されていません。");
        }
        return tenantId;
    }
}
