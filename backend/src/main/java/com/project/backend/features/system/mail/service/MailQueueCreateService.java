package com.project.backend.features.system.mail.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.dto.MailAddressSet;
import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.dto.MailTemplateQueueCreateRequest;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.entity.MailTemplate;
import com.project.backend.features.system.mail.enums.MailSendStatus;
import com.project.backend.features.system.mail.mapper.MailQueueMapper;
import com.project.backend.features.system.mail.repository.MailSendQueueRepository;
import com.project.backend.features.system.mail.service.builder.MailQueueCreateRequestBuilder;
import com.project.backend.features.system.mail.service.finder.MailRecipientGroupFinder;
import com.project.backend.features.system.mail.service.finder.MailTemplateFinder;
import com.project.backend.features.system.mail.service.resolver.MailRecipientGroupAddressResolver;
import com.project.backend.features.system.mail.service.validation.MailDuplicateSendGuard;
import com.project.backend.features.system.mail.service.validation.MailQueueCreateValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailQueueCreateService {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final MailSendQueueRepository queueRepository;
    private final MailTemplateRenderService renderService;
    private final MailRecipientGroupAddressResolver addressResolver;
    private final MailQueueMapper mapper;
    private final MailQueueCreateValidator validator;
    private final MailQueueCreateRequestBuilder requestBuilder;
    private final MailTemplateFinder templateFinder;
    private final MailRecipientGroupFinder recipientGroupFinder;
    private final MailDuplicateSendGuard duplicateSendGuard;

    @Transactional
    public MailSendQueue createDraft(MailQueueCreateRequest request) {
        validator.validateCreateRequest(request);
        duplicateSendGuard.validateBeforeCreate(request.businessKey());

        MailSendQueue queue = mapper.toEntity(request);
        queue.setStatus(MailSendStatus.DRAFT);
        queue.setRetryCount(0);
        queue.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);
        queue.setLastErrorMessage(null);

        return queueRepository.save(queue);
    }

    @Transactional
    public MailSendQueue createWaiting(MailQueueCreateRequest request) {
        MailSendQueue queue = createDraft(request);
        queue.setStatus(MailSendStatus.WAITING);
        return queue;
    }

    @Transactional
    public MailSendQueue createDraftFromTemplate(MailTemplateQueueCreateRequest request) {
        validator.validateTemplateRequest(request);

        MailTemplate template = templateFinder.getActive(request.templateKey());

        String subject = renderService.render(
                template.getSubjectTemplate(),
                request.variablesOrEmpty()
        );
        String body = renderService.render(
                template.getBodyTemplate(),
                request.variablesOrEmpty()
        );

        if (validator.hasAddresses(request.toAddresses())) {
            return createDraft(
                    requestBuilder.buildDirect(request, template, subject, body)
            );
        }

        MailRecipientGroup group = recipientGroupFinder.getActive(request.recipientGroupKey());

        MailAddressSet addresses = addressResolver.resolve(
                group,
                request.recipientKey(),
                request.ccAddresses(),
                request.bccAddresses()
        );

        if (!validator.hasAddresses(addresses.toAddresses())) {
            throw new RuntimeException("TO宛先が空です。 groupKey=" + request.recipientGroupKey());
        }

        return createDraft(
                requestBuilder.buildFromGroup(
                        request,
                        template,
                        group,
                        addresses,
                        subject,
                        body
                )
        );
    }
}
