package com.project.backend.features.system.report.service.api;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.entity.ReportHistory;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportHistoryStatus;
import com.project.backend.features.system.report.repository.ReportHistoryRepository;
import com.project.backend.features.system.report.service.builder.ReportHistoryBuilder;
import com.project.backend.features.system.report.service.resolver.ReportHistoryMessageResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportHistoryService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final ReportHistoryBuilder reportHistoryBuilder;
    private final ReportHistoryMessageResolver messageResolver;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccess(
            ReportMaster reportMaster,
            String executionId,
            Map<String, Object> requestParams,
            ReportStoredFile storedFile
    ) {
        save(
                reportMaster,
                executionId,
                requestParams,
                ReportHistoryStatus.SUCCESS,
                null,
                storedFile
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(
            ReportMaster reportMaster,
            String executionId,
            Map<String, Object> requestParams,
            Exception exception
    ) {
        save(
                reportMaster,
                executionId,
                requestParams,
                ReportHistoryStatus.FAILED,
                messageResolver.failureMessage(exception),
                null
        );
    }

    @SuppressWarnings("null")
private void save(
            ReportMaster reportMaster,
            String executionId,
            Map<String, Object> requestParams,
            ReportHistoryStatus status,
            String notes,
            ReportStoredFile storedFile
    ) {
        ReportHistory history =
                reportHistoryBuilder.build(
                        reportMaster,
                        executionId,
                        requestParams,
                        status,
                        notes,
                        storedFile
                );

        reportHistoryRepository.save(history);
    }
}