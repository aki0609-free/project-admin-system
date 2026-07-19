package com.project.backend.features.system.report.service.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.dto.MailTemplateQueueCreateRequest;
import com.project.backend.features.system.report.entity.ReportOutputFile;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportMailQueueCreateRequestBuilder {

    private final ReportMailAttachmentBuilder attachmentBuilder;

    public MailTemplateQueueCreateRequest build(ReportOutputFile file) {
        return MailTemplateQueueCreateRequest.builder()
                .templateKey(file.getMailTemplateKey())
                .recipientGroupKey(file.getRecipientGroupKey())
                .recipientKey(file.getRecipientKey())
                .mailType(file.getMailType())
                .businessKey(file.getBusinessKey())
                .toAddresses(toAddresses(file))
                .toName(file.getRecipientName())
                .attachments(List.of(attachmentBuilder.build(file)))
                .variables(buildVariables(file))
                .build();
    }

    private List<String> toAddresses(ReportOutputFile file) {
        return StringUtils.hasText(file.getRecipientEmail())
                ? List.of(file.getRecipientEmail())
                : List.of();
    }

    private Map<String, Object> buildVariables(ReportOutputFile file) {
        Map<String, Object> values = new HashMap<>();

        values.put("executionId", file.getExecutionId());
        values.put("reportCode", file.getReportCode());
        values.put("businessKey", file.getBusinessKey());
        values.put("recipientKey", file.getRecipientKey());
        values.put("recipientName", file.getRecipientName());
        values.put("recipientEmail", file.getRecipientEmail());
        values.put("fileName", file.getFileName());
        values.put("mailType", file.getMailType());

        return values;
    }
}