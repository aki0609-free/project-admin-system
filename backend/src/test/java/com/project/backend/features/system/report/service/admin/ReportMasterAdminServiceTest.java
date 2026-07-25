package com.project.backend.features.system.report.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.mapper.ReportMasterDtoMapper;
import com.project.backend.features.system.report.repository.ReportMasterRepository;
import com.project.backend.features.system.report.service.sync.ReportParamSyncService;
import com.project.backend.features.system.report.service.updater.ReportMasterUpdater;
import com.project.backend.features.system.report.service.validation.ReportMasterValidator;

class ReportMasterAdminServiceTest {

    @Test
    void delete_shouldUseApplicationClock() {
        Instant fixedInstant =
                Instant.parse("2026-12-31T15:00:01Z");
        ReportMasterRepository repository =
                mock(ReportMasterRepository.class);
        ReportMaster master = new ReportMaster();

        when(repository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(master));

        ReportMasterAdminService service =
                new ReportMasterAdminService(
                        repository,
                        mock(ReportMasterDtoMapper.class),
                        mock(ReportMasterValidator.class),
                        mock(ReportMasterUpdater.class),
                        mock(ReportParamSyncService.class),
                        Clock.fixed(
                                fixedInstant,
                                ZoneId.of("Asia/Tokyo")
                        )
                );

        service.delete(1L);

        assertThat(master.getDeletedAt())
                .isEqualTo(fixedInstant);
    }
}
