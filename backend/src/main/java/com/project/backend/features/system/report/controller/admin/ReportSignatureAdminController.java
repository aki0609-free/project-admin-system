package com.project.backend.features.system.report.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.project.backend.app.base.dto.MessageResponse;
import com.project.backend.features.system.report.dto.ReportSignatureResponse;
import com.project.backend.features.system.report.dto.ReportSignatureSaveRequest;
import com.project.backend.features.system.report.dto.ReportSignatureSaveResponse;
import com.project.backend.features.system.report.service.admin.ReportSignatureAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-signatures")
@RequiredArgsConstructor
public class ReportSignatureAdminController {

    private final ReportSignatureAdminService reportSignatureAdminService;

    @GetMapping
    public List<ReportSignatureResponse> findAll() {
        return reportSignatureAdminService.findAll();
    }

    @GetMapping("/{id}")
    public ReportSignatureResponse findById(@PathVariable Long id) {
        return reportSignatureAdminService.findById(id);
    }

    @PostMapping
    public ReportSignatureSaveResponse create(@RequestBody ReportSignatureSaveRequest request) {
        return reportSignatureAdminService.create(request);
    }

    @PutMapping("/{id}")
    public ReportSignatureSaveResponse update(
            @PathVariable Long id,
            @RequestBody ReportSignatureSaveRequest request
    ) {
        return reportSignatureAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id) {
        reportSignatureAdminService.delete(id);

        return MessageResponse.builder()
                .message("帳票署名を削除しました。")
                .build();
    }
}