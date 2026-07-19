package com.project.backend.features.system.mail.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.mail.enums.MailStorageType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail_send_attachment")
@Getter
@Setter
public class MailSendAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mail_send_queue_id", nullable = false)
    private MailSendQueue mailSendQueue;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private MailStorageType storageType = MailStorageType.LOCAL;

    @Column(name = "file_key", nullable = false, length = 1000)
    private String fileKey;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;
}