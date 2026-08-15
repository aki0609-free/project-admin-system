package com.project.backend.features.admin.business.service;

import java.net.URI;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.ExternalSupportLinkSettingResponse;
import com.project.backend.features.admin.business.dto.ExternalSupportLinkSettingSaveRequest;
import com.project.backend.features.admin.business.entity.ExternalSupportLinkSetting;
import com.project.backend.features.admin.business.repository.ExternalSupportLinkSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalSupportLinkSettingService {

    static final String DEFAULT_INCIDENT_REPORT_URL =
            "https://projectadmin1215.atlassian.net/jira/software/projects/FUYO/form/1"
                    + "?atlOrigin=eyJpIjoiYTFkN2E2NWU2YWYwNGQ2ODk4MDRmMTliM2JkMjQ3YjgiLCJwIjoiaiJ9";
    static final String DEFAULT_MANUAL_URL =
            "https://projectadmin1215.atlassian.net/wiki/spaces/"
                    + "~712020d0db24f25d734730b24dfb1508d24613/folder/19464193";

    private final ExternalSupportLinkSettingRepository repository;

    @Transactional(readOnly = true)
    public ExternalSupportLinkSettingResponse find() {
        return repository.findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                        currentTenantId(),
                        ExternalSupportLinkSetting.DEFAULT_SETTING_CODE
                )
                .map(this::toResponse)
                .orElseGet(() -> new ExternalSupportLinkSettingResponse(
                        DEFAULT_INCIDENT_REPORT_URL,
                        DEFAULT_MANUAL_URL
                ));
    }

    public ExternalSupportLinkSettingResponse save(
            ExternalSupportLinkSettingSaveRequest request
    ) {
        String incidentReportUrl = normalizeHttpsUrl(
                request.incidentReportUrl(),
                "インシデント報告のURL"
        );
        String manualUrl = normalizeHttpsUrl(request.manualUrl(), "マニュアルのURL");
        String tenantId = currentTenantId();

        ExternalSupportLinkSetting setting = repository
                .findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                        tenantId,
                        ExternalSupportLinkSetting.DEFAULT_SETTING_CODE
                )
                .orElseGet(ExternalSupportLinkSetting::new);
        setting.setSettingCode(ExternalSupportLinkSetting.DEFAULT_SETTING_CODE);
        setting.setIncidentReportUrl(incidentReportUrl);
        setting.setManualUrl(manualUrl);
        return toResponse(repository.save(setting));
    }

    private String normalizeHttpsUrl(String value, String label) {
        String normalized = value == null ? "" : value.trim();
        try {
            URI uri = URI.create(normalized);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(label + "には有効なHTTPS URLを指定してください。");
        }
    }

    private String currentTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("TenantContext に tenantId が設定されていません。");
        }
        return tenantId;
    }

    private ExternalSupportLinkSettingResponse toResponse(
            ExternalSupportLinkSetting setting
    ) {
        return new ExternalSupportLinkSettingResponse(
                setting.getIncidentReportUrl(),
                setting.getManualUrl()
        );
    }
}
