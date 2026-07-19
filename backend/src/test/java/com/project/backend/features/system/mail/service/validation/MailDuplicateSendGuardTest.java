package com.project.backend.features.system.mail.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailSendStatus;
import com.project.backend.features.system.mail.repository.MailSendQueueRepository;

class MailDuplicateSendGuardTest {

    @Test
    void validateBeforeCreate_shouldRejectSentBusinessKey() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailDuplicateSendGuard guard = new MailDuplicateSendGuard(repository);
        when(repository.existsByBusinessKeyAndStatusAndDeletedAtIsNull(
                "PAYSLIP:2026-07:1",
                MailSendStatus.SENT
        )).thenReturn(true);

        assertThatThrownBy(() -> guard.validateBeforeCreate("PAYSLIP:2026-07:1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("重複送信")
                .hasMessageContaining("PAYSLIP:2026-07:1");
    }

    @Test
    void hasSentDuplicateForUpdate_shouldIgnoreCurrentQueue() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailDuplicateSendGuard guard = new MailDuplicateSendGuard(repository);
        MailSendQueue current = queue(5L, MailSendStatus.SENT);
        current.setBusinessKey("NOTICE:1");
        when(repository.findAllByBusinessKeyAndDeletedAtIsNullOrderByIdAsc("NOTICE:1"))
                .thenReturn(List.of(current));

        assertThat(guard.hasSentDuplicateForUpdate(current)).isFalse();
    }

    @Test
    void hasSentDuplicateForUpdate_shouldDetectOtherSentQueueUnderLock() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailDuplicateSendGuard guard = new MailDuplicateSendGuard(repository);
        MailSendQueue current = queue(6L, MailSendStatus.WAITING);
        current.setBusinessKey("NOTICE:1");
        MailSendQueue sent = queue(5L, MailSendStatus.SENT);
        sent.setBusinessKey("NOTICE:1");
        when(repository.findAllByBusinessKeyAndDeletedAtIsNullOrderByIdAsc("NOTICE:1"))
                .thenReturn(List.of(sent, current));

        assertThat(guard.hasSentDuplicateForUpdate(current)).isTrue();
        verify(repository)
                .findAllByBusinessKeyAndDeletedAtIsNullOrderByIdAsc("NOTICE:1");
    }

    @Test
    void hasSentDuplicateForUpdate_shouldSkipLookup_whenBusinessKeyIsBlank() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailDuplicateSendGuard guard = new MailDuplicateSendGuard(repository);

        assertThat(guard.hasSentDuplicateForUpdate(new MailSendQueue())).isFalse();
        verifyNoInteractions(repository);
    }

    private MailSendQueue queue(Long id, MailSendStatus status) {
        MailSendQueue queue = new MailSendQueue();
        queue.setId(id);
        queue.setStatus(status);
        return queue;
    }
}
