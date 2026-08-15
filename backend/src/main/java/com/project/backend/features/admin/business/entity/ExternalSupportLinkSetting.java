package com.project.backend.features.admin.business.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "external_support_link_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_external_support_link_setting_code",
                columnNames = {"tenant_id", "setting_code"}
        )
)
@Getter
@Setter
public class ExternalSupportLinkSetting extends BaseEntity {

    public static final String DEFAULT_SETTING_CODE = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_code", nullable = false, length = 50)
    private String settingCode = DEFAULT_SETTING_CODE;

    @Column(name = "incident_report_url", nullable = false, length = 2048)
    private String incidentReportUrl;

    @Column(name = "manual_url", nullable = false, length = 2048)
    private String manualUrl;
}
