package com.project.backend.features.system.report.service.builder;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.dto.MailSendResult;
import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.dto.ReportRecipientGenerationSummary;

@Component
public class ReportBatchResultMessageBuilder {

    public String buildExportMessage() {
        return "Report出力が完了しました。";
    }

    public String buildMailMessage(
            ReportStoredFile storedFile,
            ReportRecipientGenerationSummary generationSummary,
            int queuedCount,
            MailSendResult mailResult
    ) {
        return "ReportMailバッチが完了しました。"
                + " file=" + storedFile.fileKey()
                + ", targetCount=" + generationSummary.targetCount()
                + ", successCount=" + generationSummary.successCount()
                + ", failedCount=" + generationSummary.failedCount()
                + ", skippedCount=" + generationSummary.skippedCount()
                + ", mailQueueCount=" + queuedCount
                + ", mailTargetCount=" + mailResult.targetCount()
                + ", mailSentCount=" + mailResult.sentCount()
                + ", mailFailedCount=" + mailResult.failedCount();
    }
}
