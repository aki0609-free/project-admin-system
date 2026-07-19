package com.project.backend.features.system.mail.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.service.builder.MailSendResultBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailDirectSendService {

    private final MailQueueCreateService queueCreateService;
    private final MailSendWorkerService workerService;
    private final MailSendResultBuilder resultBuilder;

    @Transactional
    public MailSendResult send(MailQueueCreateRequest request) {
        MailSendQueue queue = queueCreateService.createWaiting(request);

        boolean success = workerService.sendOne(queue);

        return resultBuilder.sent(
                1,
                success ? 1 : 0,
                success ? 0 : 1
        );
    }
}