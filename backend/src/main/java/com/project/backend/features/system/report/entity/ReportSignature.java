package com.project.backend.features.system.report.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.report.enums.ReportSignatureType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_signature")
@Getter
@Setter
public class ReportSignature extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_master_id", nullable = false)
    private ReportMaster reportMaster;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 30)
    private ReportSignatureType signatureType;

    @Column(name = "signature_name", nullable = false, length = 100)
    private String signatureName;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Lob
    @Column(
        name = "signature_image_data",
        nullable = false,
        columnDefinition = "LONGBLOB"
    )
    private byte[] signatureImageData;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Column(name = "default_flag", nullable = false)
    private Boolean defaultFlag = false;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;
}