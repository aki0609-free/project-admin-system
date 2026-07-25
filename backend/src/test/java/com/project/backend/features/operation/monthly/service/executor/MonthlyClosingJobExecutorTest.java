package com.project.backend.features.operation.monthly.service.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingReportFileRepository;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.dto.BatchJobRunResult;
import com.project.backend.features.system.batch.service.BatchExecutionService;

class MonthlyClosingJobExecutorTest {

    @Test
    void execute_shouldUseApplicationClockForGeneratedAt() {
        Instant fixedInstant =
                Instant.parse("2026-12-31T15:00:01Z");
        BatchExecutionService batchExecutionService =
                mock(BatchExecutionService.class);
        MonthlyClosingReportFileRepository repository =
                mock(MonthlyClosingReportFileRepository.class);

        when(batchExecutionService.executeNowForResult(
                org.mockito.ArgumentMatchers.eq("MONTHLY_JOB"),
                anyMap()
        )).thenReturn(BatchJobRunResult.builder()
                .executionLogId(10L)
                .result(BatchJobExecutionResult.message(
                        "completed"
                ))
                .build());

        MonthlyClosingJobExecutor executor =
                new MonthlyClosingJobExecutor(
                        batchExecutionService,
                        repository,
                        Clock.fixed(
                                fixedInstant,
                                ZoneId.of("Asia/Tokyo")
                        )
                );

        OperationReportPreview preview =
                new OperationReportPreview();
        preview.setReportCode("MONTHLY_PAY_SLIP");
        preview.setJobCode("MONTHLY_JOB");

        executor.execute(
                1L,
                preview,
                new MonthlyClosingPeriod(
                        "2026-12",
                        LocalDate.of(2026, 12, 1),
                        LocalDate.of(2026, 12, 31),
                        null
                ),
                1
        );

        ArgumentCaptor<MonthlyClosingReportFile> captor =
                ArgumentCaptor.forClass(
                        MonthlyClosingReportFile.class
                );
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getGeneratedAt())
                .isEqualTo(fixedInstant);
        assertThat(captor.getValue()
                .getBatchExecutionLogId()).isEqualTo(10L);
    }
}
