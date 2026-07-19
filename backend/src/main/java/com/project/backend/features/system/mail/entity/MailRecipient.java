package com.project.backend.features.system.mail.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.mail.enums.MailRecipientType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail_recipient")
@Getter
@Setter
public class MailRecipient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private MailRecipientGroup group;

    @Column(name = "recipient_key", length = 100)
    private String recipientKey;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private MailRecipientType recipientType = MailRecipientType.TO;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}