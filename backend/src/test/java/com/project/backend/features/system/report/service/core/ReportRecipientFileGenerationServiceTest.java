package com.project.backend.features.system.report.service.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.report.dto.ReportRecipientFileProcessResult;
import com.project.backend.features.system.report.dto.ReportRecipientGenerationSummary;
import com.project.backend.features.system.report.dto.ReportRecipientOutputGroup;
import com.project.backend.features.system.report.entity.ReportMaster;

class ReportRecipientFileGenerationServiceTest {

    @Test
    void generate_shouldContinueAfterOneRecipientFails() {
        ReportRecipientOutputGrouper grouper = mock(ReportRecipientOutputGrouper.class);
        ReportRecipientFileProcessor processor = mock(ReportRecipientFileProcessor.class);
        ReportRecipientFileGenerationService service =
                new ReportRecipientFileGenerationService(grouper, processor);
        ReportMaster reportMaster = new ReportMaster();

        ReportRecipientOutputGroup first = group("business-1");
        ReportRecipientOutputGroup second = group("business-2");
        when(grouper.group(List.of(Map.of()))).thenReturn(List.of(first, second));
        when(processor.process(reportMaster, "execution", first))
                .thenThrow(new RuntimeException("render error"));
        when(processor.process(reportMaster, "execution", second))
                .thenReturn(ReportRecipientFileProcessResult.created("PAY_SLIP"));

        ReportRecipientGenerationSummary summary = service.generate(
                reportMaster,
                "execution",
                List.of(Map.of())
        );

        assertThat(summary.targetCount()).isEqualTo(2);
        assertThat(summary.successCount()).isEqualTo(1);
        assertThat(summary.failedCount()).isEqualTo(1);
        assertThat(summary.mailTypes()).containsExactly("PAY_SLIP");
    }

    private ReportRecipientOutputGroup group(String businessKey) {
        return new ReportRecipientOutputGroup(
                businessKey,
                "1",
                "従業員1",
                "one@example.com",
                null,
                "PAY_SLIP",
                "PAY_SLIP_NOTICE",
                List.of(Map.of("amount", 100))
        );
    }
}
