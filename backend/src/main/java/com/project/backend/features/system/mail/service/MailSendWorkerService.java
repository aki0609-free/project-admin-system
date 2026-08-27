package com.project.backend.features.system.mail.service;

import java.time.Instant;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailSendStatus;
import com.project.backend.features.system.mail.service.builder.MailMessageBuilder;
import com.project.backend.features.system.mail.service.support.MailErrorMessageLimiter;
import com.project.backend.features.system.mail.service.validation.MailDuplicateSendGuard;
import com.project.backend.features.system.mail.service.validation.MailQueueValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailSendWorkerService {

    private final JavaMailSender mailSender;
    private final MailQueueValidator validator;
    private final MailMessageBuilder messageBuilder;
    private final MailErrorMessageLimiter errorMessageLimiter;
    private final MailDuplicateSendGuard duplicateSendGuard;

    @SuppressWarnings("null")
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean sendOne(MailSendQueue mail) {
        try {
            markSending(mail);

            if (duplicateSendGuard.hasSentDuplicateForUpdate(mail)) {
                markDuplicateFailed(mail);
                return false;
            }

            validator.validateBeforeSend(mail);
            mailSender.send(messageBuilder.build(mail));

            markSent(mail);
            return true;

        } catch (Exception e) {
            markFailedOrWaiting(mail, e);
            log.warn(
                    "Mail send failed. queueId={}, mailType={}, businessKey={}, status={}, retryCount={}, errorType={}",
                    mail.getId(),
                    mail.getMailType(),
                    mail.getBusinessKey(),
                    mail.getStatus(),
                    mail.getRetryCount(),
                    e.getClass().getSimpleName()
            );
            return false;
        }
    }

    private void markSending(MailSendQueue mail) {
        mail.setStatus(MailSendStatus.SENDING);
        mail.setLastErrorMessage(null);
    }

    private void markSent(MailSendQueue mail) {
        mail.setStatus(MailSendStatus.SENT);
        mail.setSentAt(Instant.now());
        mail.setLastErrorMessage(null);
    }

    private void markFailedOrWaiting(MailSendQueue mail, Exception e) {
        mail.setRetryCount(mail.getRetryCount() + 1);
        mail.setLastErrorMessage(errorMessageLimiter.limit(e.getMessage()));

        mail.setStatus(
                mail.getRetryCount() >= mail.getMaxRetryCount()
                        ? MailSendStatus.FAILED
                        : MailSendStatus.WAITING
        );
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
}
