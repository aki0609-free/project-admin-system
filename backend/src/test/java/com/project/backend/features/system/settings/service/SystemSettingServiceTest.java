package com.project.backend.features.system.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.settings.dto.SystemSettingValueSaveItem;
import com.project.backend.features.system.settings.dto.SystemSettingValueSaveRequest;
import com.project.backend.features.system.settings.entity.SystemSettingDefinition;
import com.project.backend.features.system.settings.entity.SystemSettingScope;
import com.project.backend.features.system.settings.entity.SystemSettingValue;
import com.project.backend.features.system.settings.entity.SystemSettingValueType;
import com.project.backend.features.system.settings.repository.SystemSettingDefinitionRepository;
import com.project.backend.features.system.settings.repository.SystemSettingValueRepository;

class SystemSettingServiceTest {

    private SystemSettingDefinitionRepository definitionRepository;
    private SystemSettingValueRepository valueRepository;
    private SystemSettingService service;
    private SystemSettingDefinition definition;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-test");
        definitionRepository = mock(SystemSettingDefinitionRepository.class);
        valueRepository = mock(SystemSettingValueRepository.class);
        service = new SystemSettingService(
                definitionRepository,
                valueRepository,
                new ObjectMapper()
        );
        definition = definition(10L, "MANUAL_URL", "マニュアルURL");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findAll_shouldCombineDefinitionsAndTenantValues() {
        SystemSettingValue value = new SystemSettingValue();
        value.setDefinition(definition);
        value.setSettingValue("https://example.com/manual");
        when(definitionRepository.findEffective("tenant-test"))
                .thenReturn(List.of(definition));
        when(valueRepository.findByTenantIdAndDefinitionIdInAndDeletedAtIsNull(
                "tenant-test", List.of(10L)
        )).thenReturn(List.of(value));

        var result = service.findAll();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.smallCategoryKey()).isEqualTo("MANUAL_URL");
            assertThat(item.value()).isEqualTo("https://example.com/manual");
        });
    }

    @Test
    void save_shouldPersistValidatedHttpsUrl() {
        when(definitionRepository.findAllById(List.of(10L))).thenReturn(List.of(definition));
        when(valueRepository.findByTenantIdAndDefinitionIdAndDeletedAtIsNull(
                "tenant-test", 10L
        )).thenReturn(Optional.empty());
        when(valueRepository.save(any(SystemSettingValue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(definitionRepository.findEffective("tenant-test"))
                .thenReturn(List.of(definition));
        when(valueRepository.findByTenantIdAndDefinitionIdInAndDeletedAtIsNull(
                "tenant-test", List.of(10L)
        )).thenReturn(List.of());

        service.save(new SystemSettingValueSaveRequest(List.of(
                new SystemSettingValueSaveItem(10L, " https://example.com/manual ")
        )));

        verify(valueRepository).save(any(SystemSettingValue.class));
    }

    @Test
    void save_shouldRejectNonHttpsUrl() {
        when(definitionRepository.findAllById(List.of(10L))).thenReturn(List.of(definition));

        assertThatThrownBy(() -> service.save(new SystemSettingValueSaveRequest(List.of(
                new SystemSettingValueSaveItem(10L, "http://example.com/manual")
        )))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("形式が正しくありません");
    }

    private SystemSettingDefinition definition(Long id, String smallKey, String name) {
        SystemSettingDefinition result = new SystemSettingDefinition();
        result.setId(id);
        result.setLargeCategoryKey("SYSTEM");
        result.setLargeCategoryName("システム共通設定");
        result.setMiddleCategoryKey("SUPPORT_LINK");
        result.setMiddleCategoryName("サポートリンク");
        result.setSmallCategoryKey(smallKey);
        result.setScope(SystemSettingScope.SYSTEM);
        result.setOwnerTenantId("*");
        result.setSettingName(name);
        result.setValueType(SystemSettingValueType.URL);
        result.setRequiredFlag(true);
        result.setActiveFlag(true);
        result.setDisplayOrder(10);
        return result;
    }
}
