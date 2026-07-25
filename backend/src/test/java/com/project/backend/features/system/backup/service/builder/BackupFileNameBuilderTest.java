package com.project.backend.features.system.backup.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class BackupFileNameBuilderTest {

    private final BackupFileNameBuilder builder =
            new BackupFileNameBuilder(Clock.fixed(
                    Instant.parse("2026-12-31T15:00:01Z"),
                    ZoneId.of("Asia/Tokyo")
            ));

    @Test
    void buildCsvFileName_shouldUseTokyoDateAcrossYearBoundary() {
        String fileName = builder.buildCsvFileName(
                "EMPLOYEE",
                "{targetCode}_{timestamp}.csv"
        );

        assertThat(fileName)
                .matches(
                        "EMPLOYEE_20270101_000001_000_[a-f0-9]{8}\\.csv"
                );
    }

    @Test
    void buildZipFileName_shouldKeepExistingFormat() {
        assertThat(builder.buildZipFileName())
                .matches(
                        "backup_20270101_000001_000_[a-f0-9]{8}\\.zip"
                );
    }
}
