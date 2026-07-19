package com.project.backend.features.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.application.dto.ApplicantCreateRequest;
import com.project.backend.features.application.dto.ApplicantUpdateRequest;
import com.project.backend.features.application.entity.Applicant;
import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.mapper.ApplicantMapper;
import com.project.backend.features.application.repository.ApplicantRepository;
import com.project.backend.features.application.resolver.ApplicationMediaResolver;
import com.project.backend.features.application.validator.ApplicantCodeValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicantCommandService {

    private final ApplicantRepository applicantRepository;
    private final ApplicantMapper applicantMapper;
    private final ApplicantCodeValidator applicantCodeValidator;
    private final ApplicationMediaResolver applicationMediaResolver;
    private final ApplicationMediaMetricsService applicationMediaMetricsService;

    public Long create(ApplicantCreateRequest request) {
        validateCodes(request);

        if (request.getApplicationNo() == null || request.getApplicationNo().isBlank()) {
            throw new IllegalArgumentException("applicationNo は必須です。");
        }

        if (applicantRepository.findByApplicationNo(request.getApplicationNo()).isPresent()) {
            throw new IllegalArgumentException("同じ applicationNo が既に存在します。applicationNo=" + request.getApplicationNo());
        }

        ApplicationMedia media = applicationMediaResolver.resolve(request.getApplicationMedia());

        Applicant entity = applicantMapper.toEntity(request);
        entity.setApplicationMedia(media);

        applyMediaSnapshots(entity, media);

        Applicant saved = applicantRepository.save(entity);

        if (media != null) {
            applicationMediaMetricsService.recalculate(media.getId());
        }

        return saved.getId();
    }

    @SuppressWarnings("null")
    public void update(Long id, ApplicantUpdateRequest request) {
        validateCodes(request);

        Applicant entity = applicantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("応募者が見つかりません。id=" + id));

        Long beforeMediaId = entity.getApplicationMedia() != null
                ? entity.getApplicationMedia().getId()
                : null;

        if (request.getApplicationNo() != null
                && !request.getApplicationNo().equals(entity.getApplicationNo())
                && applicantRepository.findByApplicationNo(request.getApplicationNo()).isPresent()) {
            throw new IllegalArgumentException("同じ applicationNo が既に存在します。applicationNo=" + request.getApplicationNo());
        }

        applicantMapper.update(entity, request);

        ApplicationMedia media = applicationMediaResolver.resolve(request.getApplicationMedia());
        entity.setApplicationMedia(media);

        applyMediaSnapshots(entity, media);

        applicantRepository.save(entity);

        Long afterMediaId = media != null ? media.getId() : null;

        if (beforeMediaId != null) {
            applicationMediaMetricsService.recalculate(beforeMediaId);
        }
        if (afterMediaId != null && !afterMediaId.equals(beforeMediaId)) {
            applicationMediaMetricsService.recalculate(afterMediaId);
        }
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        Applicant entity = applicantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("応募者が見つかりません。id=" + id));

        Long mediaId = entity.getApplicationMedia() != null
                ? entity.getApplicationMedia().getId()
                : null;

        applicantRepository.delete(entity);

        if (mediaId != null) {
            applicationMediaMetricsService.recalculate(mediaId);
        }
    }

    private void validateCodes(ApplicantCreateRequest request) {
        applicantCodeValidator.validateApplicantCodes(
                request.getContractType(),
                request.getGender(),
                request.getRecruitmentStatus(),
                request.getDormitoryInfo() != null ? request.getDormitoryInfo().getNeedsDormitory() : null,
                request.getDormitoryInfo() != null ? request.getDormitoryInfo().getRoomType() : null,
                request.getPreviousJobInfo() != null ? request.getPreviousJobInfo().getInsuredBefore() : null,
                request.getPreviousJobInfo() != null ? request.getPreviousJobInfo().getDormitoryExperience() : null);
    }

    private void validateCodes(ApplicantUpdateRequest request) {
        applicantCodeValidator.validateApplicantCodes(
                request.getContractType(),
                request.getGender(),
                request.getRecruitmentStatus(),
                request.getDormitoryInfo() != null ? request.getDormitoryInfo().getNeedsDormitory() : null,
                request.getDormitoryInfo() != null ? request.getDormitoryInfo().getRoomType() : null,
                request.getPreviousJobInfo() != null ? request.getPreviousJobInfo().getInsuredBefore() : null,
                request.getPreviousJobInfo() != null ? request.getPreviousJobInfo().getDormitoryExperience() : null);
    }

    private void applyMediaSnapshots(Applicant entity, ApplicationMedia media) {
        entity.setMediaNameSnapshot(media != null ? media.getMediaName() : null);

        if (entity.getContactDate() != null) {
            entity.setMediaYearMonthSnapshot(
                    entity.getContactDate().getYear() + "-" +
                            String.format("%02d", entity.getContactDate().getMonthValue()));
            return;
        }

        entity.setMediaYearMonthSnapshot(
                media != null && media.getMediaYearMonth() != null
                        ? media.getMediaYearMonth().toString()
                        : null);
    }
}