package com.project.backend.features.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.application.dto.ApplicationMediaBulkSaveRequest;
import com.project.backend.features.application.dto.ApplicationMediaBulkUpdateItem;
import com.project.backend.features.application.dto.ApplicationMediaCreateRequest;
import com.project.backend.features.application.dto.ApplicationMediaUpdateRequest;
import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.mapper.ApplicationMediaMapper;
import com.project.backend.features.application.repository.ApplicantRepository;
import com.project.backend.features.application.repository.ApplicationMediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationMediaCommandService {

    private final ApplicationMediaRepository applicationMediaRepository;
    private final ApplicationMediaMapper applicationMediaMapper;
    private final ApplicationMediaMetricsService applicationMediaMetricsService;
    private final ApplicantRepository applicantRepository;

    @SuppressWarnings("null")
    public Long create(ApplicationMediaCreateRequest request) {
        ApplicationMedia entity = applicationMediaMapper.toEntity(request);
        ApplicationMedia saved = applicationMediaRepository.save(entity);
        applicationMediaMetricsService.recalculate(saved.getId());
        return saved.getId();
    }

    @SuppressWarnings("null")
    public void update(Long id, ApplicationMediaUpdateRequest request) {
        ApplicationMedia entity = applicationMediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("応募媒体が見つかりません。id=" + id));

        applicationMediaMapper.update(entity, request);

        applicationMediaRepository.save(entity);
        applicationMediaMetricsService.recalculate(entity.getId());
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        ApplicationMedia entity = applicationMediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("応募媒体が見つかりません。id=" + id));

        if (applicantRepository.existsActiveApplicantByApplicationMediaId(id)) {
            throw new IllegalArgumentException("この応募媒体は応募者に使用されているため削除できません。");
        }

        applicationMediaRepository.delete(entity);
    }

    public void bulkSave(ApplicationMediaBulkSaveRequest request) {
        if (request.created() != null) {
            for (ApplicationMediaCreateRequest item : request.created()) {
                create(item);
            }
        }

        if (request.updated() != null) {
            for (ApplicationMediaBulkUpdateItem item : request.updated()) {
                update(
                        item.id(),
                        new ApplicationMediaUpdateRequest(
                                item.mediaName(),
                                item.mediaArea(),
                                item.mediaSlots(),
                                item.mediaYearMonth(),
                                item.cost()
                        )
                );
            }
        }

        if (request.deletedIds() != null) {
            for (Long id : request.deletedIds()) {
                delete(id);
            }
        }
    }
}