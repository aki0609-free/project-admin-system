package com.project.backend.features.system.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.backup.entity.BackupColumn;
import com.project.backend.features.system.backup.entity.BackupTarget;
import com.project.backend.features.system.backup.mapper.BackupTargetMapper;
import com.project.backend.features.system.backup.repository.BackupTargetRepository;
import com.project.backend.features.system.backup.service.validation.BackupTargetValidator;

class BackupTargetCommandServiceTest {

    @Test
    void delete_shouldApplySameClockToTargetAndColumns() {
        Instant fixedInstant =
                Instant.parse("2026-12-31T15:00:01Z");
        BackupTargetLookupService lookupService =
                mock(BackupTargetLookupService.class);
        BackupTarget target = new BackupTarget();
        BackupColumn column = new BackupColumn();
        target.addColumn(column);

        when(lookupService.find(1L))
                .thenReturn(target);

        BackupTargetCommandService service =
                new BackupTargetCommandService(
                        mock(BackupTargetRepository.class),
                        lookupService,
                        mock(BackupTargetValidator.class),
                        mock(BackupTargetMapper.class),
                        Clock.fixed(
                                fixedInstant,
                                ZoneId.of("Asia/Tokyo")
                        )
                );

        service.delete(1L);

        assertThat(target.getDeletedAt())
                .isEqualTo(fixedInstant);
        assertThat(column.getDeletedAt())
                .isEqualTo(fixedInstant);
    }
}
