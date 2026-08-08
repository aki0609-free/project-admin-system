package com.project.backend.features.admin.business.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.admin.business.dto.BusinessClosingSettingResponse;
import com.project.backend.features.admin.business.dto.BusinessClosingSettingSaveRequest;
import com.project.backend.features.admin.business.dto.DormitoryFeeSettingResponse;
import com.project.backend.features.admin.business.dto.DormitoryFeeSettingSaveRequest;
import com.project.backend.features.admin.business.dto.MonthlyClosingOutputAdminResponse;
import com.project.backend.features.admin.business.dto.MonthlyClosingOutputSaveRequest;
import com.project.backend.features.admin.business.dto.ResignationChecklistAdminResponse;
import com.project.backend.features.admin.business.dto.ResignationChecklistSaveRequest;
import com.project.backend.features.admin.business.dto.ResignationMessageSaveRequest;
import com.project.backend.features.admin.business.service.BusinessSettingService;
import com.project.backend.features.employee.dto.EmployeeResignationMessageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/business-settings")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class BusinessSettingController {

    private final BusinessSettingService service;

    @GetMapping("/dormitory-fees")
    public List<DormitoryFeeSettingResponse> findDormitoryFees() {
        return service.findDormitoryFees();
    }

    @PutMapping("/dormitory-fees")
    public List<DormitoryFeeSettingResponse> saveDormitoryFees(
            @Valid @RequestBody List<@Valid DormitoryFeeSettingSaveRequest> requests
    ) {
        return service.saveDormitoryFees(requests);
    }

    @GetMapping("/resignation-message")
    public EmployeeResignationMessageResponse findResignationMessage() {
        return service.findResignationMessage();
    }

    @PutMapping("/resignation-message")
    public EmployeeResignationMessageResponse saveResignationMessage(
            @Valid @RequestBody ResignationMessageSaveRequest request
    ) {
        return service.saveResignationMessage(request);
    }

    @GetMapping("/resignation-checklist")
    public List<ResignationChecklistAdminResponse> findChecklist() {
        return service.findChecklist();
    }

    @PostMapping("/resignation-checklist")
    public ResignationChecklistAdminResponse createChecklist(
            @Valid @RequestBody ResignationChecklistSaveRequest request
    ) {
        return service.createChecklist(request);
    }

    @PutMapping("/resignation-checklist/{id}")
    public ResignationChecklistAdminResponse updateChecklist(
            @PathVariable Long id,
            @Valid @RequestBody ResignationChecklistSaveRequest request
    ) {
        return service.updateChecklist(id, request);
    }

    @DeleteMapping("/resignation-checklist/{id}")
    public void deleteChecklist(@PathVariable Long id) {
        service.deleteChecklist(id);
    }

    @GetMapping("/closing-setting")
    public BusinessClosingSettingResponse findClosingSetting() {
        return service.findClosingSetting();
    }

    @PutMapping("/closing-setting")
    public BusinessClosingSettingResponse saveClosingSetting(
            @Valid @RequestBody BusinessClosingSettingSaveRequest request
    ) {
        return service.saveClosingSetting(request);
    }

    @GetMapping("/closing-outputs")
    public List<MonthlyClosingOutputAdminResponse> findClosingOutputs() {
        return service.findClosingOutputs();
    }

    @PutMapping("/closing-outputs")
    public List<MonthlyClosingOutputAdminResponse> saveClosingOutputs(
            @Valid @RequestBody List<@Valid MonthlyClosingOutputSaveRequest> requests
    ) {
        return service.saveClosingOutputs(requests);
    }
}
