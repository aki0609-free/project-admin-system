package com.project.backend.features.system.mail.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.dto.MailPdfSendRequest;
import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.properties.MailProperties;
import com.project.backend.features.system.mail.service.builder.MailPdfQueueCreateRequestBuilder;
import com.project.backend.features.system.mail.service.validation.MailPdfSendValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailPdfSendService {

    private final MailProperties properties;
    private final MailPdfSendValidator validator;
    private final MailPdfQueueCreateRequestBuilder builder;
    private final MailDirectSendService mailDirectSendService;

    @Transactional
    public MailSendResult send(MailPdfSendRequest request) {
        validator.validate(request, properties);

        MailQueueCreateRequest queueRequest = builder.build(request, properties);

        return mailDirectSendService.send(queueRequest);
    }
}