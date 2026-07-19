package com.project.backend.features.system.report.service.admin;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.dto.ReportMasterDetailResponse;
import com.project.backend.features.system.report.dto.ReportMasterListResponse;
import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.mapper.ReportMasterDtoMapper;
import com.project.backend.features.system.report.repository.ReportMasterRepository;
import com.project.backend.features.system.report.service.sync.ReportParamSyncService;
import com.project.backend.features.system.report.service.updater.ReportMasterUpdater;
import com.project.backend.features.system.report.service.validation.ReportMasterValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportMasterAdminService {

    private final ReportMasterRepository reportMasterRepository;
    private final ReportMasterDtoMapper reportMasterDtoMapper;
    private final ReportMasterValidator validator;
    private final ReportMasterUpdater updater;
    private final ReportParamSyncService paramSyncService;

    @Transactional(readOnly = true)
    public List<ReportMasterListResponse> findAll() {
        return reportMasterDtoMapper.toListResponseList(
                reportMasterRepository.findByDeletedAtIsNullOrderByIdAsc()
        );
    }

    @Transactional(readOnly = true)
    public ReportMasterDetailResponse findDetail(Long id) {
        ReportMaster entity = reportMasterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("帳票定義が見つかりません。 id=" + id));

        return reportMasterDtoMapper.toDetailResponse(entity);
    }

    @Transactional
    public ReportMasterDetailResponse create(ReportMasterSaveRequest request) {
        validator.validate(request);

        ReportMaster entity = new ReportMaster();

        updater.apply(entity, request);
        paramSyncService.sync(entity, request.params());

        ReportMaster saved = reportMasterRepository.save(entity);

        return reportMasterDtoMapper.toDetailResponse(saved);
    }

    @SuppressWarnings("null")
    @Transactional
    public ReportMasterDetailResponse update(Long id, ReportMasterSaveRequest request) {
        validator.validate(request);

        ReportMaster entity = reportMasterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("帳票定義が見つかりません。 id=" + id));

        updater.apply(entity, request);
        paramSyncService.sync(entity, request.params());

        ReportMaster saved = reportMasterRepository.save(entity);

        return reportMasterDtoMapper.toDetailResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        ReportMaster entity = reportMasterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("帳票定義が見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now());
    }
}