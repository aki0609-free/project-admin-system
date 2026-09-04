package com.project.backend.features.admin.business.service;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.common.closing.ClosingSettingDefaults;
import com.project.backend.common.closing.entity.ClosingSetting;
import com.project.backend.common.closing.repository.ClosingSettingRepository;
import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.admin.business.dto.BusinessClosingSettingResponse;
import com.project.backend.features.admin.business.dto.BusinessClosingSettingSaveRequest;
import com.project.backend.features.admin.business.dto.MonthlyClosingOutputAdminResponse;
import com.project.backend.features.admin.business.dto.MonthlyClosingOutputSaveRequest;
import com.project.backend.features.admin.business.dto.ResignationChecklistAdminResponse;
import com.project.backend.features.admin.business.dto.ResignationChecklistSaveRequest;
import com.project.backend.features.admin.business.dto.ResignationMessageSaveRequest;
import com.project.backend.features.employee.dto.EmployeeResignationMessageResponse;
import com.project.backend.features.employee.entity.EmployeeResignationChecklistMaster;
import com.project.backend.features.employee.entity.EmployeeResignationSetting;
import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;
import com.project.backend.features.employee.repository.EmployeeResignationSettingRepository;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessSettingService {

    private static final String PAYROLL_SETTING_CODE =
            ClosingSettingDefaults.PAYROLL_SETTING_CODE;

    private final EmployeeResignationSettingRepository resignationSettingRepository;
    private final EmployeeResignationChecklistRepository checklistRepository;
    private final ClosingSettingRepository closingSettingRepository;
    private final MonthlyClosingOutputDefinitionRepository outputDefinitionRepository;
    private final OperationReportPreviewRepository reportPreviewRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public EmployeeResignationMessageResponse findResignationMessage() {
        EmployeeResignationSetting setting = resignationSettingRepository
                .findBySettingCodeAndDeletedAtIsNull(
                        EmployeeResignationSetting.DEFAULT_SETTING_CODE
                )
                .orElseGet(EmployeeResignationSetting::new);
        return toMessageResponse(setting);
    }

    public EmployeeResignationMessageResponse saveResignationMessage(
            ResignationMessageSaveRequest request
    ) {
        EmployeeResignationSetting setting = resignationSettingRepository
                .findBySettingCodeAndDeletedAtIsNull(
                        EmployeeResignationSetting.DEFAULT_SETTING_CODE
                )
                .orElseGet(EmployeeResignationSetting::new);
        setting.setSettingCode(EmployeeResignationSetting.DEFAULT_SETTING_CODE);
        setting.setDialogTitle(request.dialogTitle().trim());
        setting.setGuidanceMessage(request.guidanceMessage().trim());
        setting.setConfirmationMessage(request.confirmationMessage().trim());
        return toMessageResponse(resignationSettingRepository.save(setting));
    }

    @Transactional(readOnly = true)
    public List<ResignationChecklistAdminResponse> findChecklist() {
        return checklistRepository
                .findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(this::toChecklistResponse)
                .toList();
    }

    public ResignationChecklistAdminResponse createChecklist(
            ResignationChecklistSaveRequest request
    ) {
        String code = normalizeCode(request.code());
        if (checklistRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new IllegalArgumentException("同じTODOコードが既に登録されています。");
        }
        EmployeeResignationChecklistMaster entity =
                new EmployeeResignationChecklistMaster();
        applyChecklist(entity, request, code);
        return toChecklistResponse(checklistRepository.save(entity));
    }

    public ResignationChecklistAdminResponse updateChecklist(
            Long id,
            ResignationChecklistSaveRequest request
    ) {
        EmployeeResignationChecklistMaster entity = checklistRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("退職TODOが見つかりません。"));
        String code = normalizeCode(request.code());
        if (!entity.getCode().equals(code)) {
            throw new IllegalArgumentException("TODOコードは作成後に変更できません。");
        }
        if (checklistRepository.existsByCodeAndIdNotAndDeletedAtIsNull(code, id)) {
            throw new IllegalArgumentException("同じTODOコードが既に登録されています。");
        }
        applyChecklist(entity, request, code);
        return toChecklistResponse(checklistRepository.save(entity));
    }

    public void deleteChecklist(Long id) {
        EmployeeResignationChecklistMaster entity = checklistRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("退職TODOが見つかりません。"));
        entity.setDeletedAt(Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public BusinessClosingSettingResponse findClosingSetting() {
        ClosingSetting setting = closingSettingRepository
                .findFirstBySettingCodeAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(
                        PAYROLL_SETTING_CODE
                )
                .orElseGet(ClosingSettingDefaults::payroll);
        return toClosingResponse(setting);
    }

    public BusinessClosingSettingResponse saveClosingSetting(
            BusinessClosingSettingSaveRequest request
    ) {
        validateDayRule(request.closingDay(), "締日");
        validateDayRule(request.paymentDay(), "支払日");

        ClosingSetting setting = closingSettingRepository
                .findFirstBySettingCodeAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(
                        PAYROLL_SETTING_CODE
                )
                .orElseGet(ClosingSetting::new);
        setting.setSettingCode(PAYROLL_SETTING_CODE);
        applyDayRule(setting, request.closingDay(), true);
        applyDayRule(setting, request.paymentDay(), false);
        setting.setActiveFlag(true);
        return toClosingResponse(closingSettingRepository.save(setting));
    }

    @Transactional(readOnly = true)
    public List<MonthlyClosingOutputAdminResponse> findClosingOutputs() {
        Map<String, MonthlyClosingOutputDefinition> definitions =
                outputDefinitionRepository
                        .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                                MonthlyClosingOutputType.REPORT
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                MonthlyClosingOutputDefinition::getOutputCode,
                                Function.identity()
                        ));

        return reportPreviewRepository
                .findByOperationTypeAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        OperationType.MONTHLY
                )
                .stream()
                .map(preview -> toClosingOutputResponse(
                        preview,
                        definitions.get(preview.getReportCode())
                ))
                .toList();
    }

    public List<MonthlyClosingOutputAdminResponse> saveClosingOutputs(
            List<MonthlyClosingOutputSaveRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("締め帳票設定は1件以上必要です。");
        }
        if (requests.stream().map(MonthlyClosingOutputSaveRequest::reportCode)
                .distinct().count() != requests.size()) {
            throw new IllegalArgumentException("締め帳票コードが重複しています。");
        }

        for (MonthlyClosingOutputSaveRequest request : requests) {
            OperationReportPreview preview = reportPreviewRepository
                    .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                            OperationType.MONTHLY,
                            request.reportCode()
                    )
                    .orElseThrow(() -> new IllegalArgumentException(
                            "月次帳票が見つかりません。reportCode=" + request.reportCode()
                    ));
            if (Boolean.TRUE.equals(request.activeFlag())
                    && !Boolean.TRUE.equals(preview.getActiveFlag())) {
                throw new IllegalArgumentException(
                        "帳票管理で無効な帳票は締め帳票として有効化できません。reportCode="
                                + request.reportCode()
                );
            }

            MonthlyClosingOutputDefinition definition = outputDefinitionRepository
                    .findByOutputTypeAndOutputCodeAndDeletedAtIsNull(
                            MonthlyClosingOutputType.REPORT,
                            request.reportCode()
                    )
                    .orElseGet(MonthlyClosingOutputDefinition::new);
            definition.setOutputType(MonthlyClosingOutputType.REPORT);
            definition.setOutputCode(request.reportCode());
            definition.setExecutionOrder(request.executionOrder());
            definition.setRequiredFlag(true);
            definition.setActiveFlag(request.activeFlag());
            definition.setBackupRetentionYears(request.backupRetentionYears());
            outputDefinitionRepository.save(definition);
        }
        return findClosingOutputs();
    }

    private EmployeeResignationMessageResponse toMessageResponse(
            EmployeeResignationSetting setting
    ) {
        return new EmployeeResignationMessageResponse(
                setting.getDialogTitle(),
                setting.getGuidanceMessage(),
                setting.getConfirmationMessage()
        );
    }

    private ResignationChecklistAdminResponse toChecklistResponse(
            EmployeeResignationChecklistMaster entity
    ) {
        return new ResignationChecklistAdminResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isRequiredFlag(),
                entity.getDisplayOrder(),
                entity.isActiveFlag()
        );
    }

    private void applyChecklist(
            EmployeeResignationChecklistMaster entity,
            ResignationChecklistSaveRequest request,
            String code
    ) {
        entity.setCode(code);
        entity.setName(request.name().trim());
        entity.setDescription(normalizeNullable(request.description()));
        entity.setRequiredFlag(Boolean.TRUE.equals(request.requiredFlag()));
        entity.setDisplayOrder(request.displayOrder());
        entity.setActiveFlag(Boolean.TRUE.equals(request.activeFlag()));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BusinessClosingSettingResponse toClosingResponse(ClosingSetting setting) {
        return new BusinessClosingSettingResponse(
                setting.getId(),
                setting.getSettingCode(),
                new DayRule(
                        setting.getClosingDayType(),
                        setting.getClosingDayValue(),
                        setting.getClosingMonthOffset()
                ),
                new DayRule(
                        setting.getPaymentDayType(),
                        setting.getPaymentDayValue(),
                        setting.getPaymentMonthOffset()
                ),
                setting.isActiveFlag()
        );
    }

    private void validateDayRule(DayRule rule, String label) {
        if (rule == null || rule.type() == null) {
            throw new IllegalArgumentException(label + "ルールは必須です。");
        }
        if (rule.type() == DayRuleType.DAY_OF_MONTH
                && (rule.value() == null || rule.value() < 1 || rule.value() > 31)) {
            throw new IllegalArgumentException(label + "の日は1〜31で指定してください。");
        }
        int monthOffset = rule.monthOffset() == null ? 0 : rule.monthOffset();
        if (monthOffset < -12 || monthOffset > 12) {
            throw new IllegalArgumentException(label + "の月オフセットは-12〜12で指定してください。");
        }
    }

    private void applyDayRule(ClosingSetting setting, DayRule rule, boolean closing) {
        Integer value = rule.type() == DayRuleType.END_OF_MONTH ? null : rule.value();
        Integer monthOffset = rule.monthOffset() == null ? 0 : rule.monthOffset();
        if (closing) {
            setting.setClosingDayType(rule.type());
            setting.setClosingDayValue(value);
            setting.setClosingMonthOffset(monthOffset);
        } else {
            setting.setPaymentDayType(rule.type());
            setting.setPaymentDayValue(value);
            setting.setPaymentMonthOffset(monthOffset);
        }
    }

    private MonthlyClosingOutputAdminResponse toClosingOutputResponse(
            OperationReportPreview preview,
            MonthlyClosingOutputDefinition definition
    ) {
        boolean configured = definition != null;
        return new MonthlyClosingOutputAdminResponse(
                configured ? definition.getId() : null,
                preview.getReportCode(),
                preview.getReportName(),
                preview.getJobCode(),
                preview.getOutputType(),
                configured ? definition.getExecutionOrder() : preview.getDisplayOrder(),
                !configured || Boolean.TRUE.equals(definition.getRequiredFlag()),
                configured
                        ? Boolean.TRUE.equals(definition.getActiveFlag())
                        : Boolean.TRUE.equals(preview.getActiveFlag()),
                configured ? definition.getBackupRetentionYears() : null
        );
    }
}
