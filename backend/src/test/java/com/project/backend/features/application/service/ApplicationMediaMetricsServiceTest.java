package com.project.backend.features.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.repository.ApplicantRepository;
import com.project.backend.features.application.repository.ApplicationMediaRepository;

class ApplicationMediaMetricsServiceTest {

    private final ApplicationMediaRepository mediaRepository = mock(ApplicationMediaRepository.class);
    private final ApplicantRepository applicantRepository = mock(ApplicantRepository.class);
    private final ApplicationMediaMetricsService service =
            new ApplicationMediaMetricsService(mediaRepository, applicantRepository);

    @Test
    void recalculateUpdatesHiresAndCostPerHire() {
        ApplicationMedia media = ApplicationMedia.builder()
                .id(10L)
                .cost(new BigDecimal("100000"))
                .build();
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(applicantRepository.countHiredByApplicationMediaId(10L)).thenReturn(3L);

        service.recalculate(10L);

        assertThat(media.getHires()).isEqualTo(3);
        assertThat(media.getUnitPrice()).isEqualByComparingTo("33333.33");
        verify(mediaRepository).save(media);
    }

    @Test
    void recalculateClearsCostPerHireWhenThereAreNoHires() {
        ApplicationMedia media = ApplicationMedia.builder()
                .id(10L)
                .cost(new BigDecimal("100000"))
                .hires(2)
                .unitPrice(new BigDecimal("50000"))
                .build();
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(applicantRepository.countHiredByApplicationMediaId(10L)).thenReturn(0L);

        service.recalculate(10L);

        assertThat(media.getHires()).isZero();
        assertThat(media.getUnitPrice()).isNull();
    }
}
