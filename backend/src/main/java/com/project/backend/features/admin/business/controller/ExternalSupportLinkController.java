package com.project.backend.features.admin.business.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.admin.business.dto.ExternalSupportLinkSettingResponse;
import com.project.backend.features.admin.business.service.ExternalSupportLinkSettingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/support-links")
@RequiredArgsConstructor
public class ExternalSupportLinkController {

    private final ExternalSupportLinkSettingService service;

    @GetMapping
    public ExternalSupportLinkSettingResponse find() {
        return service.find();
    }
}
