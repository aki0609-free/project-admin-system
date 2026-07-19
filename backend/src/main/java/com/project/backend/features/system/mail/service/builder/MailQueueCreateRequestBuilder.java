package com.project.backend.features.system.mail.service.builder;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.dto.MailAddressSet;
import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.dto.MailTemplateQueueCreateRequest;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;
import com.project.backend.features.system.mail.entity.MailTemplate;

@Component
public class MailQueueCreateRequestBuilder {

    public MailQueueCreateRequest buildDirect(
            MailTemplateQueueCreateRequest request,
            MailTemplate template,
            String subject,
            String body
    ) {
        return MailQueueCreateRequest.builder()
                .mailType(request.mailType())
                .businessKey(request.businessKey())
                .fromAddress(template.getFromAddress())
                .fromName(template.getFromName())
                .toAddresses(request.toAddresses())
                .toName(request.toName())
                .ccAddresses(request.ccAddresses())
                .bccAddresses(request.bccAddresses())
                .subject(subject)
                .body(body)
                .htmlFlag(template.isHtmlFlag())
                .attachments(request.attachmentsOrEmpty())
                .build();
    }

    public MailQueueCreateRequest buildFromGroup(
            MailTemplateQueueCreateRequest request,
            MailTemplate template,
            MailRecipientGroup group,
            MailAddressSet addresses,
            String subject,
            String body
    ) {
        return MailQueueCreateRequest.builder()
                .mailType(request.mailType())
                .businessKey(request.businessKey())
                .fromAddress(template.getFromAddress())
                .fromName(template.getFromName())
                .toAddresses(addresses.toAddresses())
                .toName(group.getGroupName())
                .ccAddresses(addresses.ccAddresses())
                .bccAddresses(addresses.bccAddresses())
                .subject(subject)
                .body(body)
                .htmlFlag(template.isHtmlFlag())
                .attachments(request.attachmentsOrEmpty())
                .build();
    }
}