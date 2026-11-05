package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;
import com.project.backend.features.employee.repository.EmployeeResignationSettingRepository;
import com.project.backend.features.system.settings.service.SystemSettingService;

class EmployeeResignationChecklistQueryServiceTest {

    private EmployeeResignationChecklistRepository repository;
    private EmployeeResignationSettingRepository settingRepository;
    private GenericResignationChecklistResolver genericResolver;
    private SystemSettingService systemSettingService;
    private EmployeeResignationChecklistQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmployeeResignationChecklistRepository.class);
        settingRepository = mock(EmployeeResignationSettingRepository.class);
        genericResolver = mock(GenericResignationChecklistResolver.class);
        systemSettingService = mock(SystemSettingService.class);
        service = new EmployeeResignationChecklistQueryService(
                repository,
                settingRepository,
                genericResolver,
                systemSettingService
        );
    }

    @Test
    void findConfiguration_shouldUseCommonSettingsAndGenericChecklist() {
        when(settingRepository.findBySettingCodeAndDeletedAtIsNull("DEFAULT"))
                .thenReturn(Optional.empty());
        when(systemSettingService.findValue(
                "BUSINESS", "RESIGNATION_DIALOG", "DIALOG_TITLE"
        )).thenReturn("退職手続き");
        when(systemSettingService.findValue(
                "BUSINESS", "RESIGNATION_DIALOG", "GUIDANCE_MESSAGE"
        )).thenReturn("確認してください。");
        when(systemSettingService.findValue(
                "BUSINESS", "RESIGNATION_DIALOG", "CONFIRMATION_MESSAGE"
        )).thenReturn("実行します。");
        when(genericResolver.resolve()).thenReturn(
                new GenericResignationChecklistResolver.Resolution(
                        true,
                        List.of(new GenericResignationChecklistResolver.ChecklistItem(
                                101L,
                                "RETURN_CARD",
                                "社員証返却",
                                "返却済みであること",
                                true,
                                10
                        ))
                )
        );

        var result = service.findConfiguration();

        assertThat(result.message().dialogTitle()).isEqualTo("退職手続き");
        assertThat(result.message().guidanceMessage()).isEqualTo("確認してください。");
        assertThat(result.message().confirmationMessage()).isEqualTo("実行します。");
        assertThat(result.checklist()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(101L);
            assertThat(item.code()).isEqualTo("RETURN_CARD");
            assertThat(item.name()).isEqualTo("社員証返却");
            assertThat(item.requiredFlag()).isTrue();
        });
        verifyNoInteractions(repository);
    }
}
