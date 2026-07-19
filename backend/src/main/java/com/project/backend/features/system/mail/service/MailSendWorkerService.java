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

@Service
@RequiredArgsConstructor
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
