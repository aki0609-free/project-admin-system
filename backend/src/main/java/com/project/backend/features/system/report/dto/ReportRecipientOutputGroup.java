package com.project.backend.features.system.report.dto;

import java.util.List;
import java.util.Map;

public record ReportRecipientOutputGroup(
        String businessKey,
        String recipientKey,
        String recipientName,
        String recipientEmail,
        String recipientGroupKey,
        String mailType,
        String mailTemplateKey,
        List<Map<String, Object>> rows
) {
}
