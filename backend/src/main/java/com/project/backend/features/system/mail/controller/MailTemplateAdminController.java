package com.project.backend.features.system.mail.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.mail.dto.MailTemplatePreviewRequest;
import com.project.backend.features.system.mail.dto.MailTemplatePreviewResponse;
import com.project.backend.features.system.mail.dto.MailTemplateResponse;
import com.project.backend.features.system.mail.dto.MailTemplateSaveRequest;
import com.project.backend.features.system.mail.service.MailTemplateAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/mail-templates")
@RequiredArgsConstructor
public class MailTemplateAdminController {

    private final MailTemplateAdminService service;

    @GetMapping
    public List<MailTemplateResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MailTemplateResponse findDetail(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @PostMapping
    public MailTemplateResponse create(
            @RequestBody MailTemplateSaveRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MailTemplateResponse update(
            @PathVariable Long id,
            @RequestBody MailTemplateSaveRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/preview")
    public MailTemplatePreviewResponse preview(
            @RequestBody MailTemplatePreviewRequest request
    ) {
        return service.preview(request);
    }
}
