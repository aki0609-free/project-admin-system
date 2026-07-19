package com.project.backend.features.system.mail.service.builder;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.dto.MailSendResult;

@Component
public class MailSendResultBuilder {

    public MailSendResult activated(int targetCount, int activatedCount, int failedCount) {
        return MailSendResult.builder()
                .targetCount(targetCount)
                .sentCount(activatedCount)
                .failedCount(failedCount)
                .message("DRAFTメールをWAITINGに変更しました。")
                .build();
    }

    public MailSendResult sent(int targetCount, int sentCount, int failedCount) {
        return MailSendResult.builder()
                .targetCount(targetCount)
                .sentCount(sentCount)
                .failedCount(failedCount)
                .message("メール送信処理が完了しました。")
                .build();
    }

    public MailSendResult activateAndSent(
            MailSendResult activateResult,
            MailSendResult sendResult
    ) {
        return MailSendResult.builder()
                .targetCount(activateResult.targetCount())
                .sentCount(sendResult.sentCount())
                .failedCount(activateResult.failedCount() + sendResult.failedCount())
                .message("DRAFT有効化とメール送信が完了しました。")
                .build();
    }
}