package com.project.backend.features.system.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.service.builder.MailSendResultBuilder;

class MailDirectSendServiceTest {

    @Test
    void send_shouldReturnFailureResult_whenWorkerFails() {
        MailQueueCreateService queueCreateService = mock(MailQueueCreateService.class);
        MailSendWorkerService workerService = mock(MailSendWorkerService.class);
        MailSendQueue queue = new MailSendQueue();
        MailQueueCreateRequest request = MailQueueCreateRequest.builder().build();
        when(queueCreateService.createWaiting(request)).thenReturn(queue);
        when(workerService.sendOne(queue)).thenReturn(false);
        MailDirectSendService service = new MailDirectSendService(
                queueCreateService,
                workerService,
                new MailSendResultBuilder()
        );

        MailSendResult result = service.send(request);

        assertThat(result.sentCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.message()).contains("失敗");
    }
}
