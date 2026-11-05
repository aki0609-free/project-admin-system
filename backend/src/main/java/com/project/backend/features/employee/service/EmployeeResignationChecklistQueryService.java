package com.project.backend.features.employee.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeResignationChecklistResponse;
import com.project.backend.features.employee.dto.EmployeeResignationConfigurationResponse;
import com.project.backend.features.employee.dto.EmployeeResignationMessageResponse;
import com.project.backend.features.employee.entity.EmployeeResignationChecklistMaster;
import com.project.backend.features.employee.entity.EmployeeResignationSetting;
import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;
import com.project.backend.features.employee.repository.EmployeeResignationSettingRepository;
import com.project.backend.features.system.settings.service.SystemSettingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeResignationChecklistQueryService {

    private final EmployeeResignationChecklistRepository repository;
    private final EmployeeResignationSettingRepository settingRepository;
    private final GenericResignationChecklistResolver genericResolver;
    private final SystemSettingService systemSettingService;

    public List<EmployeeResignationChecklistResponse> findAllActive() {
        GenericResignationChecklistResolver.Resolution generic = genericResolver.resolve();
        if (generic.configured()) {
            return generic.items().stream()
                    .map(item -> EmployeeResignationChecklistResponse.builder()
                            .id(item.id())
                            .code(item.code())
                            .name(item.name())
                            .description(item.description())
                            .requiredFlag(item.requiredFlag())
                            .displayOrder(item.displayOrder())
                            .build())
                    .toList();
        }
        return repository
                .findAllByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EmployeeResignationConfigurationResponse findConfiguration() {
        EmployeeResignationSetting setting = settingRepository
                .findBySettingCodeAndDeletedAtIsNull(
                        EmployeeResignationSetting.DEFAULT_SETTING_CODE
                )
                .orElseGet(EmployeeResignationSetting::new);

        return new EmployeeResignationConfigurationResponse(
                new EmployeeResignationMessageResponse(
                        settingValue("DIALOG_TITLE", setting.getDialogTitle()),
                        settingValue("GUIDANCE_MESSAGE", setting.getGuidanceMessage()),
                        settingValue("CONFIRMATION_MESSAGE", setting.getConfirmationMessage())
                ),
                findAllActive()
        );
    }

    private String settingValue(String smallKey, String fallback) {
        String value = systemSettingService.findValue(
                "BUSINESS", "RESIGNATION_DIALOG", smallKey
        );
        return value == null || value.isBlank() ? fallback : value;
    }

    private EmployeeResignationChecklistResponse toResponse(
            EmployeeResignationChecklistMaster entity
    ) {
        return EmployeeResignationChecklistResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .requiredFlag(entity.isRequiredFlag())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
