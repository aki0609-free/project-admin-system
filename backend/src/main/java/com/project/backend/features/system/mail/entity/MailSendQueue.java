package com.project.backend.features.system.mail.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.mail.enums.MailSendStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail_send_queue", indexes = {
                @Index(name = "idx_mail_queue_status", columnList = "status"),
                @Index(name = "idx_mail_queue_business_key", columnList = "business_key")
})
@Getter
@Setter
public class MailSendQueue extends BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "mail_type", length = 100)
        private String mailType;

        @Column(name = "business_key", length = 255)
        private String businessKey;

        @Column(name = "from_address", nullable = false, length = 255)
        private String fromAddress;

        @Column(name = "from_name", length = 255)
        private String fromName;

        @Column(name = "to_address", nullable = false, length = 1000)
        private String toAddress;

        @Column(name = "to_name", length = 255)
        private String toName;

        @Column(name = "cc", length = 1000)
        private String cc;

        @Column(name = "bcc", length = 1000)
        private String bcc;

        @Column(name = "subject", nullable = false, length = 500)
        private String subject;

        @Lob
        @Column(name = "body", nullable = false)
        private String body;

        @Column(name = "html_flag", nullable = false)
        private boolean htmlFlag = false;

        @OneToMany(mappedBy = "mailSendQueue", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<MailSendAttachment> attachments = new ArrayList<>();

        public void addAttachment(MailSendAttachment attachment) {
                attachment.setMailSendQueue(this);
                this.attachments.add(attachment);
        }

        public void clearAttachments() {
                this.attachments.clear();
        }

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 30)
        private MailSendStatus status = MailSendStatus.DRAFT;

        @Column(name = "retry_count", nullable = false)
        private int retryCount = 0;

        @Column(name = "max_retry_count", nullable = false)
        private int maxRetryCount = 3;

        @Column(name = "sent_at")
        private Instant sentAt;

        @Column(name = "last_error_message", length = 4000)
        private String lastErrorMessage;
}