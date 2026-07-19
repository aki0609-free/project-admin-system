package com.project.backend.features.system.report.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.entity.ReportSignature;
import com.project.backend.features.system.report.repository.ReportSignatureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportSignatureDefaultService {

    private final ReportSignatureRepository reportSignatureRepository;

    @Transactional
    public void normalizeDefaultFlag(ReportSignature target, Long selfId) {
        if (!Boolean.TRUE.equals(target.getDefaultFlag())) {
            return;
        }

        List<ReportSignature> siblings = selfId == null
                ? reportSignatureRepository.findByReportMasterId(target.getReportMaster().getId())
                : reportSignatureRepository.findByReportMasterIdAndIdNot(
                        target.getReportMaster().getId(),
                        selfId
                );

        for (ReportSignature sibling : siblings) {
            if (Boolean.TRUE.equals(sibling.getDefaultFlag())) {
                sibling.setDefaultFlag(false);
                reportSignatureRepository.save(sibling);
            }
        }
    }
}