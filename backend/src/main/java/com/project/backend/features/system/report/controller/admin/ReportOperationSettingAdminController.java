package com.project.backend.features.system.report.controller.admin;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.features.admin.business.dto.MonthlyClosingOutputAdminResponse;
import com.project.backend.features.admin.business.dto.MonthlyClosingOutputSaveRequest;
import com.project.backend.features.admin.business.dto.PreviewReportSettingResponse;
import com.project.backend.features.admin.business.dto.PreviewReportSettingSaveRequest;
import com.project.backend.features.admin.business.service.BusinessSettingService;
import com.project.backend.features.admin.business.service.PreviewReportSettingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 帳票管理画面から締め帳票とHTMLプレビュー帳票の運用設定を管理する。 */
@RestController
@RequestMapping("/api/admin/reports/operation-settings")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class ReportOperationSettingAdminController {

    private final BusinessSettingService businessSettingService;
    private final PreviewReportSettingService previewReportSettingService;

    @GetMapping("/closing-outputs")
    public List<MonthlyClosingOutputAdminResponse> findClosingOutputs() {
        return businessSettingService.findClosingOutputs();
    }

    @PutMapping("/closing-outputs")
    public List<MonthlyClosingOutputAdminResponse> saveClosingOutputs(
            @Valid @RequestBody List<@Valid MonthlyClosingOutputSaveRequest> requests
    ) {
        return businessSettingService.saveClosingOutputs(requests);
    }

    @GetMapping("/preview-reports")
    public List<PreviewReportSettingResponse> findPreviewReports() {
        return previewReportSettingService.findAll();
    }

    @PostMapping(
            value = "/preview-reports",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public PreviewReportSettingResponse savePreviewReport(
            @Valid @RequestPart("definition") PreviewReportSettingSaveRequest request,
            @RequestPart(value = "template", required = false) MultipartFile template
    ) {
        return previewReportSettingService.save(request, template);
    }
}
