package com.project.backend.features.system.report.service.validation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportOutputFile;
import com.project.backend.features.system.report.enums.ReportOutputFileStatus;
import com.project.backend.features.system.report.repository.ReportOutputFileRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportOutputFileDuplicateChecker {

    private static final List<ReportOutputFileStatus> BLOCKING_STATUSES = List.of(
            ReportOutputFileStatus.CREATED,
            ReportOutputFileStatus.MAIL_QUEUED
    );

    private final ReportOutputFileRepository repository;

    public boolean isDuplicate(ReportOutputFile file) {
        if (file == null) {
            return false;
        }

        if (!StringUtils.hasText(file.getBusinessKey())) {
            return false;
        }

        return isDuplicate(file.getBusinessKey());
    }

    public boolean isDuplicate(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            return false;
        }

        return repository.existsByBusinessKeyAndStatusInAndDeletedAtIsNull(
                businessKey,
                BLOCKING_STATUSES
        );
    }
}
