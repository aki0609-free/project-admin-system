package com.project.backend.features.system.report.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
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

    @Test
    void create_shouldRejectDuplicateReportCode() {
        ReportMasterRepository repository =
                mock(ReportMasterRepository.class);
        ReportMasterValidator validator =
                mock(ReportMasterValidator.class);
        ReportMasterSaveRequest request =
                mock(ReportMasterSaveRequest.class);
        when(request.reportCode()).thenReturn("MONTHLY_PAY_SLIP");
        when(repository.existsByReportCodeAndDeletedAtIsNull(
                "MONTHLY_PAY_SLIP"
        )).thenReturn(true);

        ReportMasterAdminService service = service(
                repository,
                validator
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重複");
        verify(repository, never()).save(any());
    }

    @Test
    void update_shouldRejectChangingReportCode() {
        ReportMasterRepository repository =
                mock(ReportMasterRepository.class);
        ReportMasterValidator validator =
                mock(ReportMasterValidator.class);
        ReportMasterSaveRequest request =
                mock(ReportMasterSaveRequest.class);
        ReportMaster master = new ReportMaster();
        master.setReportCode("MONTHLY_PAY_SLIP");
        when(request.reportCode()).thenReturn("OTHER_REPORT");
        when(repository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(master));

        ReportMasterAdminService service = service(
                repository,
                validator
        );

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("作成後に変更できません");
        verify(repository, never()).save(any());
    }

    private ReportMasterAdminService service(
            ReportMasterRepository repository,
            ReportMasterValidator validator
    ) {
        return new ReportMasterAdminService(
                repository,
                mock(ReportMasterDtoMapper.class),
                validator,
                mock(ReportMasterUpdater.class),
                mock(ReportParamSyncService.class),
                Clock.systemUTC()
        );
    }
}
