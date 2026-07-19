package com.project.backend.features.system.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.mail.dto.MailSendResult;
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

class MailSendServiceTest {

    @Test
    void activateAndSendByIds_shouldSendOnlyRequestedQueues() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailQueueValidator validator = mock(MailQueueValidator.class);
        MailSendWorkerService worker = mock(MailSendWorkerService.class);
        MailSendService service = service(repository, validator, worker);
        MailSendQueue requested = queue(10L);
        MailSendQueue unrelated = queue(99L);
        List<Long> queueIds = List.of(10L);
        when(repository.findAllByIdInAndDeletedAtIsNullOrderByIdAsc(queueIds))
                .thenReturn(List.of(requested));
        when(worker.sendOne(requested)).thenReturn(true);

        MailSendResult result = service.activateAndSendByIds(queueIds);

        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        verify(validator).validateBeforeWaiting(requested);
        verify(worker).sendOne(requested);
        verify(worker, never()).sendOne(unrelated);
    }

    @Test
    void activateAndSendByIds_shouldNotSearchGlobalWaitingQueues_whenIdsAreEmpty() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailSendWorkerService worker = mock(MailSendWorkerService.class);
        MailSendService service = service(
                repository,
                mock(MailQueueValidator.class),
                worker
        );

        MailSendResult result = service.activateAndSendByIds(List.of());

        assertThat(result.targetCount()).isZero();
        assertThat(result.sentCount()).isZero();
        assertThat(result.failedCount()).isZero();
        verify(worker, never()).sendOne(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retryFailed_shouldReuseSameQueueAndResetRetryCycle() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailDuplicateSendGuard duplicateGuard = mock(MailDuplicateSendGuard.class);
        MailSendQueue failed = queue(20L);
        failed.setStatus(MailSendStatus.FAILED);
        failed.setRetryCount(3);
        failed.setLastErrorMessage("SMTP error");
        when(repository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(failed));
        MailSendService service = service(
                repository,
                mock(MailQueueValidator.class),
                mock(MailSendWorkerService.class),
                duplicateGuard
        );

        service.retryFailed(20L);

        assertThat(failed.getId()).isEqualTo(20L);
        assertThat(failed.getStatus()).isEqualTo(MailSendStatus.WAITING);
        assertThat(failed.getRetryCount()).isZero();
        assertThat(failed.getLastErrorMessage()).isNull();
    }

    @Test
    void retryFailed_shouldRejectWhenSameBusinessKeyWasAlreadySent() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailDuplicateSendGuard duplicateGuard = mock(MailDuplicateSendGuard.class);
        MailSendQueue failed = queue(30L);
        failed.setBusinessKey("PAYSLIP:2026-07:1");
        failed.setStatus(MailSendStatus.FAILED);
        failed.setRetryCount(3);
        when(repository.findByIdAndDeletedAtIsNull(30L)).thenReturn(Optional.of(failed));
        when(duplicateGuard.hasSentDuplicateForUpdate(failed)).thenReturn(true);
        when(duplicateGuard.message(failed.getBusinessKey())).thenReturn("duplicate");
        MailSendService service = service(
                repository,
                mock(MailQueueValidator.class),
                mock(MailSendWorkerService.class),
                duplicateGuard
        );

        assertThatThrownBy(() -> service.retryFailed(30L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("duplicate");

        assertThat(failed.getStatus()).isEqualTo(MailSendStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(3);
    }

    @Test
    void activateAndSendByIds_shouldFailDraftWithoutSending_whenBusinessKeyWasAlreadySent() {
        MailSendQueueRepository repository = mock(MailSendQueueRepository.class);
        MailSendWorkerService worker = mock(MailSendWorkerService.class);
        MailDuplicateSendGuard duplicateGuard = mock(MailDuplicateSendGuard.class);
        MailErrorMessageLimiter limiter = mock(MailErrorMessageLimiter.class);
        MailSendQueue duplicate = queue(40L);
        duplicate.setBusinessKey("PAYSLIP:2026-07:1");
        duplicate.setMaxRetryCount(3);
        when(repository.findAllByIdInAndDeletedAtIsNullOrderByIdAsc(List.of(40L)))
                .thenReturn(List.of(duplicate));
        when(duplicateGuard.hasSentDuplicateForUpdate(duplicate)).thenReturn(true);
        when(duplicateGuard.message(duplicate.getBusinessKey())).thenReturn("duplicate");
        when(limiter.limit(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        MailSendService service = new MailSendService(
                repository,
                mock(MailQueueValidator.class),
                worker,
                mock(MailProperties.class),
                mock(MailQueueCreateService.class),
                mock(MailTestSendValidator.class),
                mock(MailTestQueueCreateRequestBuilder.class),
                new MailSendResultBuilder(),
                limiter,
                duplicateGuard
        );

        MailSendResult result = service.activateAndSendByIds(List.of(40L));

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(duplicate.getStatus()).isEqualTo(MailSendStatus.FAILED);
        assertThat(duplicate.getRetryCount()).isEqualTo(3);
        assertThat(duplicate.getLastErrorMessage()).isEqualTo("duplicate");
        verify(worker, never()).sendOne(duplicate);
    }

    private MailSendService service(
            MailSendQueueRepository repository,
            MailQueueValidator validator,
            MailSendWorkerService worker
    ) {
        return service(
                repository,
                validator,
                worker,
                mock(MailDuplicateSendGuard.class)
        );
    }

    private MailSendService service(
            MailSendQueueRepository repository,
            MailQueueValidator validator,
            MailSendWorkerService worker,
            MailDuplicateSendGuard duplicateGuard
    ) {
        return new MailSendService(
                repository,
                validator,
                worker,
                mock(MailProperties.class),
                mock(MailQueueCreateService.class),
                mock(MailTestSendValidator.class),
                mock(MailTestQueueCreateRequestBuilder.class),
                new MailSendResultBuilder(),
                mock(MailErrorMessageLimiter.class),
                duplicateGuard
        );
    }

    private MailSendQueue queue(Long id) {
        MailSendQueue queue = new MailSendQueue();
        queue.setId(id);
        queue.setStatus(MailSendStatus.DRAFT);
        return queue;
    }
}
