package com.project.backend.features.system.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailSendStatus;
import com.project.backend.features.system.mail.service.builder.MailMessageBuilder;
import com.project.backend.features.system.mail.service.support.MailErrorMessageLimiter;
import com.project.backend.features.system.mail.service.validation.MailDuplicateSendGuard;
import com.project.backend.features.system.mail.service.validation.MailQueueValidator;

class MailSendWorkerServiceTest {

    @Test
    void sendOne_shouldFailWithoutSmtpSend_whenSameBusinessKeyWasAlreadySent() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailQueueValidator validator = mock(MailQueueValidator.class);
        MailMessageBuilder messageBuilder = mock(MailMessageBuilder.class);
        MailErrorMessageLimiter limiter = mock(MailErrorMessageLimiter.class);
        MailDuplicateSendGuard duplicateGuard = mock(MailDuplicateSendGuard.class);
        MailSendQueue mail = new MailSendQueue();
        mail.setId(2L);
        mail.setBusinessKey("PAYSLIP:2026-07:1");
        mail.setStatus(MailSendStatus.WAITING);
        mail.setMaxRetryCount(3);
        when(duplicateGuard.hasSentDuplicateForUpdate(mail)).thenReturn(true);
        when(duplicateGuard.message(mail.getBusinessKey())).thenReturn("duplicate");
        when(limiter.limit(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        MailSendWorkerService service = new MailSendWorkerService(
                mailSender,
                validator,
                messageBuilder,
                limiter,
                duplicateGuard
        );

        boolean result = service.sendOne(mail);

        assertThat(result).isFalse();
        assertThat(mail.getStatus()).isEqualTo(MailSendStatus.FAILED);
        assertThat(mail.getRetryCount()).isEqualTo(3);
        assertThat(mail.getLastErrorMessage()).isEqualTo("duplicate");
        verify(validator, never()).validateBeforeSend(mail);
        verifyNoInteractions(mailSender);
    }
}
