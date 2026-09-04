package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.ExternalSupportLinkSettingSaveRequest;
import com.project.backend.features.admin.business.entity.ExternalSupportLinkSetting;
import com.project.backend.features.admin.business.repository.ExternalSupportLinkSettingRepository;

class ExternalSupportLinkSettingServiceTest {

    private ExternalSupportLinkSettingRepository repository;
    private ExternalSupportLinkSettingService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-test");
        repository = mock(ExternalSupportLinkSettingRepository.class);
        service = new ExternalSupportLinkSettingService(repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void find_shouldNotEmbedTenantSpecificDefaultsWhenSettingDoesNotExist() {
        when(repository.findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                "tenant-test",
                ExternalSupportLinkSetting.DEFAULT_SETTING_CODE
        )).thenReturn(Optional.empty());

        var result = service.find();

        assertThat(result.incidentReportUrl()).isEmpty();
        assertThat(result.manualUrl()).isEmpty();
    }

    @Test
    void save_shouldPersistNormalizedHttpsUrlsForCurrentTenant() {
        when(repository.findByTenantIdAndSettingCodeAndDeletedAtIsNull(
                "tenant-test",
                ExternalSupportLinkSetting.DEFAULT_SETTING_CODE
        )).thenReturn(Optional.empty());
        when(repository.save(any(ExternalSupportLinkSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.save(new ExternalSupportLinkSettingSaveRequest(
                " https://example.atlassian.net/jira/form/1 ",
                "https://example.atlassian.net/wiki/manual"
        ));

        assertThat(result.incidentReportUrl())
                .isEqualTo("https://example.atlassian.net/jira/form/1");
        verify(repository).save(any(ExternalSupportLinkSetting.class));
    }

    @Test
    void save_shouldRejectNonHttpsUrl() {
        assertThatThrownBy(() -> service.save(
                new ExternalSupportLinkSettingSaveRequest(
                        "http://example.atlassian.net/jira/form/1",
                        "https://example.atlassian.net/wiki/manual"
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS URL");
    }
}
