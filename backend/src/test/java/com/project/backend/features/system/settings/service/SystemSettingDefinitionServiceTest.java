package com.project.backend.features.system.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.settings.dto.SystemSettingDefinitionCreateRequest;
import com.project.backend.features.system.settings.entity.SystemSettingDefinition;
import com.project.backend.features.system.settings.entity.SystemSettingScope;
import com.project.backend.features.system.settings.repository.SystemSettingDefinitionRepository;
import com.project.backend.features.system.settings.repository.SystemSettingValueRepository;

class SystemSettingDefinitionServiceTest {

    private SystemSettingDefinitionRepository definitionRepository;
    private SystemSettingService settingService;
    private SystemSettingDefinitionService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-test");
        definitionRepository = mock(SystemSettingDefinitionRepository.class);
        settingService = mock(SystemSettingService.class);
        service = new SystemSettingDefinitionService(
                definitionRepository,
                mock(SystemSettingValueRepository.class),
                settingService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_shouldCreateTenantOwnedDefinition() {
        var request = createRequest();
        when(definitionRepository.existsEffectiveKey(
                "tenant-test", "CUSTOM", "NOTIFICATION", "DESTINATION"
        )).thenReturn(false);
        when(definitionRepository.save(any(SystemSettingDefinition.class)))
                .thenAnswer(invocation -> {
                    SystemSettingDefinition definition = invocation.getArgument(0);
                    definition.setId(100L);
                    return definition;
                });

        var result = service.create(request);

        assertThat(result.scope()).isEqualTo("TENANT");
        assertThat(result.editable()).isTrue();
        verify(definitionRepository).save(any(SystemSettingDefinition.class));
        verify(settingService).saveValueForDefinition(
                any(SystemSettingDefinition.class),
                org.mockito.ArgumentMatchers.eq("test-value")
        );
    }

    @Test
    void create_shouldRejectKeyAlreadyUsedBySystemDefinition() {
        when(definitionRepository.existsEffectiveKey(
                "tenant-test", "CUSTOM", "NOTIFICATION", "DESTINATION"
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に登録");
    }

    @Test
    void delete_shouldRejectSystemDefinition() {
        when(definitionRepository.findByIdAndScopeAndOwnerTenantIdAndDeletedAtIsNull(
                1L, SystemSettingScope.TENANT, "tenant-test"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("編集可能");
    }

    private SystemSettingDefinitionCreateRequest createRequest() {
        return new SystemSettingDefinitionCreateRequest(
                "CUSTOM",
                "会社独自設定",
                "NOTIFICATION",
                "通知",
                "DESTINATION",
                "通知先",
                "通知先を登録します。",
                "STRING",
                false,
                null,
                null,
                100,
                false,
                true,
                "test-value"
        );
    }
}
