package com.project.backend.features.system.mail.service.builder;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.dto.MailTestSendRequest;
import com.project.backend.features.system.mail.properties.MailProperties;

@Component
public class MailTestQueueCreateRequestBuilder {

    public MailQueueCreateRequest build(
            MailTestSendRequest request,
            MailProperties properties
    ) {
        return MailQueueCreateRequest.builder()
                .mailType("TEST_MAIL")
                .businessKey("TEST_MAIL:" + Instant.now().toEpochMilli())
                .fromAddress(properties.getFromAddress())
                .fromName(properties.getFromName())
                .toAddresses(request.toAddressesOrEmpty())
                .toName(request.toNameOrNull())
                .ccAddresses(request.ccAddressesOrEmpty())
                .bccAddresses(request.bccAddressesOrEmpty())
                .subject(request.subject())
                .body(request.body())
                .htmlFlag(request.htmlFlag())
                .attachments(request.attachmentsOrEmpty())
                .build();
    }
}