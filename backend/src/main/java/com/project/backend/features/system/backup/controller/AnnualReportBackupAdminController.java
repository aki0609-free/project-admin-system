package com.project.backend.features.system.backup.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.admin.business.dto.AnnualReportBackupSettingResponse;
import com.project.backend.features.admin.business.dto.AnnualReportBackupSettingSaveRequest;
import com.project.backend.features.admin.business.service.AnnualReportBackupSettingService;
import com.project.backend.features.operation.monthly.dto.AnnualReportBackupResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/backup/annual-reports")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class AnnualReportBackupAdminController {

    private final AnnualReportBackupSettingService service;

    @GetMapping("/setting")
    public AnnualReportBackupSettingResponse findSetting() {
        return service.find();
    }

    @PutMapping("/setting")
    public AnnualReportBackupSettingResponse saveSetting(
            @Valid @RequestBody AnnualReportBackupSettingSaveRequest request
    ) {
        return service.save(request);
    }

    @PostMapping("/{fiscalYear}/execute")
    public AnnualReportBackupResult execute(
            @PathVariable int fiscalYear
    ) {
        return service.execute(fiscalYear);
    }
}
