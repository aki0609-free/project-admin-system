package com.project.backend.features.system.report.service.core;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportParam;
import com.project.backend.features.system.report.repository.ReportMasterRepository;
import com.project.backend.features.system.report.repository.ReportParamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportDefinitionService {

    private final ReportMasterRepository reportMasterRepository;
    private final ReportParamRepository reportParamRepository;

    public ReportMaster getActiveReport(String reportCode) {
        return reportMasterRepository.findByReportCodeAndActiveFlagTrueAndDeletedAtIsNull(reportCode)
                .orElseThrow(() -> new RuntimeException("帳票定義が見つかりません: " + reportCode));
    }

    public List<ReportParam> getActiveParams(Long reportMasterId) {
        return reportParamRepository.findByReportMasterIdAndActiveFlagTrueOrderByDisplayOrderAscIdAsc(reportMasterId);
    }
}