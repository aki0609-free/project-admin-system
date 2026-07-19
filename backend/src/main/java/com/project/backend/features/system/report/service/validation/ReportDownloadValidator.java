package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportHistory;
import com.project.backend.features.system.report.service.core.ReportStorageService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportDownloadValidator {

    private final ReportStorageService reportStorageService;

    public void validateHistoryFile(ReportHistory history) {
        if (history == null) {
            throw new RuntimeException("帳票履歴が未設定です。");
        }

        if (history.getStorageType() == null) {
            throw new RuntimeException("storageType がありません。 historyId=" + history.getId());
        }

        if (!StringUtils.hasText(history.getStoredFileKey())) {
            throw new RuntimeException("storedFileKey がありません。 historyId=" + history.getId());
        }

        if (!StringUtils.hasText(history.getStoredFileName())) {
            throw new RuntimeException("storedFileName がありません。 historyId=" + history.getId());
        }

        if (!StringUtils.hasText(history.getMimeType())) {
            throw new RuntimeException("mimeType がありません。 historyId=" + history.getId());
        }

        if (!reportStorageService.exists(
                history.getStorageType(),
                history.getStoredFileKey()
        )) {
            throw new RuntimeException(
                    "保存済み帳票ファイルが見つかりません。 historyId="
                            + history.getId()
                            + ", fileKey="
                            + history.getStoredFileKey()
            );
        }
    }
}
