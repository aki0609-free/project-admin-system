package com.project.backend.features.system.report.service.core;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.dto.ReportDownloadFile;
import com.project.backend.features.system.report.entity.ReportHistory;
import com.project.backend.features.system.report.repository.ReportHistoryRepository;
import com.project.backend.features.system.report.service.validation.ReportDownloadValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportDownloadService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final ReportStorageService reportStorageService;
    private final ReportDownloadValidator validator;

    @SuppressWarnings("null")
    public ReportDownloadFile downloadHistoryFile(Long historyId) {
        ReportHistory history = reportHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("帳票履歴が見つかりません。 id=" + historyId));

        validator.validateHistoryFile(history);

        byte[] data = reportStorageService.load(
                history.getStorageType(),
                history.getStoredFileKey()
        );

        return new ReportDownloadFile(
                history.getStoredFileName(),
                history.getMimeType(),
                data
        );
    }
}