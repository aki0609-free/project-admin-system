package com.project.backend.features.system.mail.entity;

import com.project.backend.app.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail_template")
@Getter
@Setter
public class MailTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "from_address", nullable = false, length = 255)
    private String fromAddress;

    @Column(name = "from_name", length = 255)
    private String fromName;

    @Column(name = "subject_template", nullable = false, length = 500)
    private String subjectTemplate;

    @Lob
    @Column(name = "body_template", nullable = false)
    private String bodyTemplate;

    @Column(name = "html_flag", nullable = false)
    private boolean htmlFlag = false;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}