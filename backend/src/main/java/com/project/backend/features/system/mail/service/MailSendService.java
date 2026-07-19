package com.project.backend.features.system.mail.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.dto.MailTestSendRequest;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailSendStatus;
import com.project.backend.features.system.mail.properties.MailProperties;
import com.project.backend.features.system.mail.repository.MailSendQueueRepository;
import com.project.backend.features.system.mail.service.builder.MailSendResultBuilder;
import com.project.backend.features.system.mail.service.builder.MailTestQueueCreateRequestBuilder;
import com.project.backend.features.system.mail.service.support.MailErrorMessageLimiter;
import com.project.backend.features.system.mail.service.validation.MailDuplicateSendGuard;
import com.project.backend.features.system.mail.service.validation.MailQueueValidator;
import com.project.backend.features.system.mail.service.validation.MailTestSendValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailSendService {

    private final MailSendQueueRepository repository;
    private final MailQueueValidator queueValidator;
    private final MailSendWorkerService workerService;
    private final MailProperties properties;
    private final MailQueueCreateService queueCreateService;
    private final MailTestSendValidator testSendValidator;
    private final MailTestQueueCreateRequestBuilder testRequestBuilder;
    private final MailSendResultBuilder resultBuilder;
    private final MailErrorMessageLimiter errorMessageLimiter;
    private final MailDuplicateSendGuard duplicateSendGuard;

    @Transactional
    public MailSendResult activateDraftByMailType(String mailType) {
        List<MailSendQueue> mails =
                repository.findByMailTypeAndStatusAndDeletedAtIsNullOrderByIdAsc(
                        mailType,
                        MailSendStatus.DRAFT
                );

        return activateDrafts(mails);
    }

    @Transactional
    public MailSendResult activateDraftByBusinessKey(String businessKey) {
        List<MailSendQueue> mails = repository.findByBusinessKeyAndDeletedAtIsNull(businessKey)
                .stream()
                .filter(mail -> mail.getStatus() == MailSendStatus.DRAFT)
                .toList();

        return activateDrafts(mails);
    }

    @Transactional
    public MailSendResult sendWaitingMails() {
        return sendMails(
                repository.findTop50ByStatusAndDeletedAtIsNullOrderByIdAsc(
                        MailSendStatus.WAITING
                )
        );
    }

    @Transactional
    public MailSendResult sendWaitingMailsByMailType(String mailType) {
        return sendMails(
                repository.findByMailTypeAndStatusAndDeletedAtIsNullOrderByIdAsc(
                        mailType,
                        MailSendStatus.WAITING
                )
        );
    }

    @Transactional
    public MailSendResult activateAndSendByMailType(String mailType) {
        return resultBuilder.activateAndSent(
                activateDraftByMailType(mailType),
                sendWaitingMailsByMailType(mailType)
        );
    }

    @Transactional
    public MailSendResult activateAndSendByIds(List<Long> queueIds) {
        if (queueIds == null || queueIds.isEmpty()) {
            return resultBuilder.activateAndSent(
                    resultBuilder.activated(0, 0, 0),
                    resultBuilder.sent(0, 0, 0)
            );
        }

        List<MailSendQueue> mails =
                repository.findAllByIdInAndDeletedAtIsNullOrderByIdAsc(queueIds)
                        .stream()
                        .filter(mail -> mail.getStatus() == MailSendStatus.DRAFT)
                        .toList();

        MailSendResult activateResult = activateDrafts(mails);
        List<MailSendQueue> waitingMails = mails.stream()
                .filter(mail -> mail.getStatus() == MailSendStatus.WAITING)
                .toList();
        MailSendResult sendResult = sendMails(waitingMails);

        return resultBuilder.activateAndSent(
                activateResult,
                sendResult
        );
    }

    @Transactional
    public void retryFailed(Long id) {
        MailSendQueue mail = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("メールキューが見つかりません。 id=" + id));

        if (mail.getStatus() != MailSendStatus.FAILED) {
            throw new RuntimeException("FAILED のメールのみ再送できます。 id=" + id);
        }

        if (duplicateSendGuard.hasSentDuplicateForUpdate(mail)) {
            throw new RuntimeException(
                    duplicateSendGuard.message(mail.getBusinessKey())
            );
        }

        mail.setStatus(MailSendStatus.WAITING);
        mail.setRetryCount(0);
        mail.setLastErrorMessage(null);
    }

    @Transactional
    public MailSendResult sendTestMail(MailTestSendRequest request) {
        testSendValidator.validate(request, properties);

        MailSendQueue mail = queueCreateService.createWaiting(
                testRequestBuilder.build(request, properties)
        );

        return sendMails(List.of(mail));
    }

    private MailSendResult activateDrafts(List<MailSendQueue> mails) {
        int activated = 0;
        int failed = 0;

        for (MailSendQueue mail : mails) {
            if (activateDraft(mail)) {
                activated++;
            } else {
                failed++;
            }
        }

        return resultBuilder.activated(mails.size(), activated, failed);
    }

    private boolean activateDraft(MailSendQueue mail) {
        try {
            if (duplicateSendGuard.hasSentDuplicateForUpdate(mail)) {
                markDuplicateFailed(mail);
                return false;
            }

            queueValidator.validateBeforeWaiting(mail);
            mail.setStatus(MailSendStatus.WAITING);
            mail.setLastErrorMessage(null);
            return true;
        } catch (Exception e) {
            mail.setStatus(MailSendStatus.FAILED);
            mail.setRetryCount(mail.getRetryCount() + 1);
            mail.setLastErrorMessage(errorMessageLimiter.limit(e.getMessage()));
            return false;
        }
    }

    private void markDuplicateFailed(MailSendQueue mail) {
        mail.setStatus(MailSendStatus.FAILED);
        mail.setRetryCount(mail.getMaxRetryCount());
        mail.setLastErrorMessage(
                errorMessageLimiter.limit(
                        duplicateSendGuard.message(mail.getBusinessKey())
                )
        );
    }

    private MailSendResult sendMails(List<MailSendQueue> mails) {
        int sent = 0;
        int failed = 0;

        for (MailSendQueue mail : mails) {
            if (workerService.sendOne(mail)) {
                sent++;
            } else {
                failed++;
            }
        }

        return resultBuilder.sent(mails.size(), sent, failed);
    }
}
