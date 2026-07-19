package com.project.backend.features.system.report.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.dto.ReportSignatureResponse;
import com.project.backend.features.system.report.dto.ReportSignatureSaveRequest;
import com.project.backend.features.system.report.dto.ReportSignatureSaveResponse;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportSignature;
import com.project.backend.features.system.report.mapper.ReportSignatureMapper;
import com.project.backend.features.system.report.repository.ReportMasterRepository;
import com.project.backend.features.system.report.repository.ReportSignatureRepository;
import com.project.backend.features.system.report.service.updater.ReportSignatureUpdater;
import com.project.backend.features.system.report.service.validation.ReportSignatureValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportSignatureAdminService {

    private final ReportSignatureRepository reportSignatureRepository;
    private final ReportMasterRepository reportMasterRepository;
    private final ReportSignatureMapper reportSignatureMapper;
    private final ReportSignatureValidator validator;
    private final ReportSignatureUpdater updater;
    private final ReportSignatureDefaultService defaultService;

    public List<ReportSignatureResponse> findAll() {
        return reportSignatureMapper.toResponseList(
                reportSignatureRepository.findAllByOrderByIdAsc()
        );
    }

    public ReportSignatureResponse findById(Long id) {
        @SuppressWarnings("null")
        ReportSignature entity = reportSignatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帳票署名が見つかりません。 id=" + id));

        return reportSignatureMapper.toResponse(entity);
    }

    @Transactional
    public ReportSignatureSaveResponse create(ReportSignatureSaveRequest request) {
        validator.validateSaveRequest(request, null);

        @SuppressWarnings("null")
        ReportMaster reportMaster = reportMasterRepository.findById(request.reportMasterId())
                .orElseThrow(() -> new RuntimeException("帳票マスタが見つかりません。 id=" + request.reportMasterId()));

        ReportSignature entity = new ReportSignature();

        updater.apply(entity, request, reportMaster, true);
        defaultService.normalizeDefaultFlag(entity, null);

        ReportSignature saved = reportSignatureRepository.save(entity);

        return ReportSignatureSaveResponse.builder()
                .id(saved.getId())
                .message("帳票署名を作成しました。")
                .build();
    }

    @Transactional
    public ReportSignatureSaveResponse update(Long id, ReportSignatureSaveRequest request) {
        validator.validateSaveRequest(request, id);

        @SuppressWarnings("null")
        ReportSignature entity = reportSignatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帳票署名が見つかりません。 id=" + id));

        @SuppressWarnings("null")
        ReportMaster reportMaster = reportMasterRepository.findById(request.reportMasterId())
                .orElseThrow(() -> new RuntimeException("帳票マスタが見つかりません。 id=" + request.reportMasterId()));

        updater.apply(entity, request, reportMaster, false);
        defaultService.normalizeDefaultFlag(entity, id);

        @SuppressWarnings("null")
        ReportSignature saved = reportSignatureRepository.save(entity);

        return ReportSignatureSaveResponse.builder()
                .id(saved.getId())
                .message("帳票署名を更新しました。")
                .build();
    }

    @SuppressWarnings("null")
    @Transactional
    public void delete(Long id) {
        @SuppressWarnings("null")
        ReportSignature entity = reportSignatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帳票署名が見つかりません。 id=" + id));

        reportSignatureRepository.delete(entity);
    }
}