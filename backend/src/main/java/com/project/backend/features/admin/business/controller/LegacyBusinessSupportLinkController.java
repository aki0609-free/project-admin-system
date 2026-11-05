package com.project.backend.features.admin.business.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.settings.dto.SupportLinkSettingResponse;
import com.project.backend.features.system.settings.dto.SupportLinkSettingSaveRequest;
import com.project.backend.features.system.settings.service.SupportLinkSettingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 旧フロントエンドのキャッシュ互換用アダプター。
 * 設定本体は system/settings ドメインで管理する。
 */
@Deprecated(forRemoval = true)
@RestController
@RequestMapping("/api/admin/business-settings/external-support-links")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class LegacyBusinessSupportLinkController {

    private final SupportLinkSettingService supportLinkSettingService;

    @GetMapping
    public SupportLinkSettingResponse find() {
        return supportLinkSettingService.find();
    }

    @PutMapping
    public SupportLinkSettingResponse save(
            @Valid @RequestBody SupportLinkSettingSaveRequest request
    ) {
        return supportLinkSettingService.save(request);
    }
}
