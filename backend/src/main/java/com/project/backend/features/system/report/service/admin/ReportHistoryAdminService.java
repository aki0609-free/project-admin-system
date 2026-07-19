package com.project.backend.features.system.report.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.dto.ReportHistoryDetailResponse;
import com.project.backend.features.system.report.dto.ReportHistoryResponse;
import com.project.backend.features.system.report.entity.ReportHistory;
import com.project.backend.features.system.report.mapper.ReportHistoryMapper;
import com.project.backend.features.system.report.repository.ReportHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportHistoryAdminService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final ReportHistoryMapper reportHistoryMapper;

    public List<ReportHistoryResponse> findAll() {
        return reportHistoryMapper.toResponseList(
                reportHistoryRepository.findAllByOrderByIdDesc()
        );
    }

    public List<ReportHistoryResponse> findByReportMasterId(Long reportMasterId) {
        return reportHistoryMapper.toResponseList(
                reportHistoryRepository.findByReportMasterIdOrderByIdDesc(reportMasterId)
        );
    }

    @SuppressWarnings("null")
public ReportHistoryDetailResponse findById(Long id) {
        ReportHistory entity = reportHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帳票履歴が見つかりません。 id=" + id));

        return reportHistoryMapper.toDetailResponse(entity);
    }
}