package com.project.backend.features.system.batch.service.executor;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.mail.service.MailSendService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailBatchJobExecutor implements BatchJobExecutor {

    private final MailSendService mailSendService;

    @Override
    public BatchJobType supportType() {
        return BatchJobType.MAIL;
    }

    @Override
    public BatchJobExecutionResult execute(BatchJobExecutionContext context) {
        MailSendResult result;

        if (context.targetCode() != null && !context.targetCode().isBlank()) {
            result = mailSendService.sendWaitingMailsByMailType(context.targetCode());
        } else {
            result = mailSendService.sendWaitingMails();
        }

        String message = result.message()
                + " targetCount=" + result.targetCount()
                + ", sentCount=" + result.sentCount()
                + ", failedCount=" + result.failedCount();

        return BatchJobExecutionResult.message(message);
    }
}