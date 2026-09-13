package com.project.backend.features.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.mapper.ApplicationMediaMapper;
import com.project.backend.features.application.repository.ApplicantRepository;
import com.project.backend.features.application.repository.ApplicationMediaRepository;

class ApplicationMediaCommandServiceTest {

    private final ApplicationMediaRepository mediaRepository = mock(ApplicationMediaRepository.class);
    private final ApplicationMediaMapper mediaMapper = mock(ApplicationMediaMapper.class);
    private final ApplicationMediaMetricsService metricsService = mock(ApplicationMediaMetricsService.class);
    private final ApplicantRepository applicantRepository = mock(ApplicantRepository.class);
    private final ApplicationMediaCommandService service = new ApplicationMediaCommandService(
            mediaRepository,
            mediaMapper,
            metricsService,
            applicantRepository
    );

    @Test
    void deleteRejectsMediaReferencedByAnApplicant() {
        ApplicationMedia media = ApplicationMedia.builder().id(10L).mediaName("求人媒体A").build();
        when(mediaRepository.findById(10L)).thenReturn(Optional.of(media));
        when(applicantRepository.existsActiveApplicantByApplicationMediaId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("応募者に使用されている");

        verify(mediaRepository, never()).delete(media);
    }
}
