package com.project.backend.features.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.application.dto.ApplicantCreateRequest;
import com.project.backend.features.application.dto.ApplicantUpdateRequest;
import com.project.backend.features.application.entity.Applicant;
import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.mapper.ApplicantMapper;
import com.project.backend.features.application.repository.ApplicantRepository;
import com.project.backend.features.application.resolver.ApplicationMediaResolver;
import com.project.backend.features.application.validator.ApplicantCodeValidator;

class ApplicantCommandServiceTest {

    private final ApplicantRepository applicantRepository = mock(ApplicantRepository.class);
    private final ApplicantMapper applicantMapper = mock(ApplicantMapper.class);
    private final ApplicantCodeValidator applicantCodeValidator = mock(ApplicantCodeValidator.class);
    private final ApplicationMediaResolver applicationMediaResolver = mock(ApplicationMediaResolver.class);
    private final ApplicationMediaMetricsService applicationMediaMetricsService =
            mock(ApplicationMediaMetricsService.class);
    private final ApplicantCommandService service = new ApplicantCommandService(
            applicantRepository,
            applicantMapper,
            applicantCodeValidator,
            applicationMediaResolver,
            applicationMediaMetricsService
    );

    @Test
    void createLinksMediaStoresSnapshotsAndRecalculatesMetrics() {
        ApplicantCreateRequest request = createRequest("A-001", LocalDate.of(2026, 9, 13));
        ApplicationMedia media = media(10L, "求人媒体A", YearMonth.of(2026, 9));
        Applicant entity = new Applicant();
        Applicant saved = new Applicant();
        saved.setId(100L);

        when(applicantRepository.findByApplicationNo("A-001")).thenReturn(Optional.empty());
        when(applicationMediaResolver.resolve(request.getApplicationMedia())).thenReturn(media);
        when(applicantMapper.toEntity(request)).thenReturn(entity);
        when(applicantRepository.save(entity)).thenReturn(saved);

        Long result = service.create(request);

        assertThat(result).isEqualTo(100L);
        assertThat(entity.getApplicationMedia()).isSameAs(media);
        assertThat(entity.getMediaNameSnapshot()).isEqualTo("求人媒体A");
        assertThat(entity.getMediaYearMonthSnapshot()).isEqualTo("2026-09");
        verify(applicationMediaMetricsService).recalculate(10L);
    }

    @Test
    void updateRecalculatesBothMediaWhenRelationChanges() {
        ApplicationMedia before = media(10L, "求人媒体A", YearMonth.of(2026, 8));
        ApplicationMedia after = media(20L, "求人媒体B", YearMonth.of(2026, 9));
        Applicant entity = new Applicant();
        entity.setId(100L);
        entity.setApplicationNo("A-001");
        entity.setApplicationMedia(before);
        ApplicantUpdateRequest request = new ApplicantUpdateRequest();
        request.setApplicationNo("A-001");
        request.setName("応募者テスト");
        request.setContactDate(LocalDate.of(2026, 9, 13));

        when(applicantRepository.findById(100L)).thenReturn(Optional.of(entity));
        when(applicationMediaResolver.resolve(request.getApplicationMedia())).thenReturn(after);

        service.update(100L, request);

        assertThat(entity.getApplicationMedia()).isSameAs(after);
        assertThat(entity.getMediaNameSnapshot()).isEqualTo("求人媒体B");
        assertThat(entity.getMediaYearMonthSnapshot()).isEqualTo("2026-09");
        verify(applicantRepository).save(entity);
        verify(applicationMediaMetricsService).recalculate(10L);
        verify(applicationMediaMetricsService).recalculate(20L);
    }

    @Test
    void deleteRecalculatesLinkedMedia() {
        ApplicationMedia media = media(10L, "求人媒体A", YearMonth.of(2026, 9));
        Applicant entity = new Applicant();
        entity.setId(100L);
        entity.setApplicationMedia(media);
        when(applicantRepository.findById(100L)).thenReturn(Optional.of(entity));

        service.delete(100L);

        verify(applicantRepository).delete(entity);
        verify(applicationMediaMetricsService).recalculate(10L);
    }

    private ApplicantCreateRequest createRequest(String applicationNo, LocalDate contactDate) {
        ApplicantCreateRequest request = new ApplicantCreateRequest();
        request.setApplicationNo(applicationNo);
        request.setName("応募者テスト");
        request.setContactDate(contactDate);
        return request;
    }

    private ApplicationMedia media(Long id, String name, YearMonth yearMonth) {
        return ApplicationMedia.builder()
                .id(id)
                .mediaName(name)
                .mediaYearMonth(yearMonth)
                .build();
    }
}
