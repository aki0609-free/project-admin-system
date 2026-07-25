package com.project.backend.features.system.backup.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupRequest;
import com.project.backend.features.system.backup.entity.BackupHistory;
import com.project.backend.features.system.backup.enums.BackupHistoryStatus;

class BackupHistoryBuilderTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-12-31T15:00:01Z");

    private final BackupHistoryBuilder builder =
            new BackupHistoryBuilder(Clock.fixed(
                    FIXED_INSTANT,
                    ZoneId.of("Asia/Tokyo")
            ));

    @Test
    void buildSuccess_shouldUseApplicationClock() {
        BackupHistory history = builder.buildSuccess(
                request(),
                BackupExecutionResult.builder()
                        .fileName("backup.csv")
                        .contentType("text/csv")
                        .data(new byte[] {1, 2, 3})
                        .zipOutput(false)
                        .build()
        );

        assertThat(history.getStatus())
                .isEqualTo(BackupHistoryStatus.SUCCESS);
        assertThat(history.getExecutedAt())
                .isEqualTo(FIXED_INSTANT);
    }

    @Test
    void buildFailure_shouldUseApplicationClock() {
        BackupHistory history = builder.buildFailure(
                request(),
                new IllegalStateException("failed")
        );

        assertThat(history.getStatus())
                .isEqualTo(BackupHistoryStatus.FAILED);
        assertThat(history.getExecutedAt())
                .isEqualTo(FIXED_INSTANT);
        assertThat(history.getErrorMessage())
                .isEqualTo("failed");
    }

    private BackupRequest request() {
        return BackupRequest.builder()
                .targetCodes(List.of("EMPLOYEE"))
                .encoding("UTF-8")
                .build();
    }
}
