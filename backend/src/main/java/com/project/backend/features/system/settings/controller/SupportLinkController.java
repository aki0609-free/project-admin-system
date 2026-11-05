package com.project.backend.features.system.settings.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.settings.dto.SupportLinkSettingResponse;
import com.project.backend.features.system.settings.service.SupportLinkSettingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/support-links")
@RequiredArgsConstructor
public class SupportLinkController {

    private final SupportLinkSettingService service;

    @GetMapping
    public SupportLinkSettingResponse find() {
        return service.find();
    }
}
