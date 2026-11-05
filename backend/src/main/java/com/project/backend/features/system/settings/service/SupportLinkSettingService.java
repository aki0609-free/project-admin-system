package com.project.backend.features.system.settings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.settings.dto.SupportLinkSettingResponse;
import com.project.backend.features.system.settings.dto.SupportLinkSettingSaveRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportLinkSettingService {

    public static final String LARGE_KEY = "SYSTEM";
    public static final String MIDDLE_KEY = "SUPPORT_LINK";
    public static final String MANUAL_URL_KEY = "MANUAL_URL";
    public static final String INCIDENT_REPORT_URL_KEY = "INCIDENT_REPORT_URL";

    private final SystemSettingService systemSettingService;

    @Transactional(readOnly = true)
    public SupportLinkSettingResponse find() {
        return new SupportLinkSettingResponse(
                systemSettingService.findValue(LARGE_KEY, MIDDLE_KEY, INCIDENT_REPORT_URL_KEY),
                systemSettingService.findValue(LARGE_KEY, MIDDLE_KEY, MANUAL_URL_KEY)
        );
    }

    public SupportLinkSettingResponse save(SupportLinkSettingSaveRequest request) {
        systemSettingService.saveValue(
                LARGE_KEY, MIDDLE_KEY, INCIDENT_REPORT_URL_KEY, request.incidentReportUrl()
        );
        systemSettingService.saveValue(
                LARGE_KEY, MIDDLE_KEY, MANUAL_URL_KEY, request.manualUrl()
        );
        return find();
    }
}
