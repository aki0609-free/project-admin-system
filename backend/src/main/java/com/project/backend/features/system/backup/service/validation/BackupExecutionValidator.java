package com.project.backend.features.system.backup.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.project.backend.features.system.backup.dto.BackupRequest;

@Component
public class BackupExecutionValidator {

    public void validate(BackupRequest request) {
        if (request == null) {
            throw new RuntimeException("バックアップリクエストが不正です。");
        }

        if (CollectionUtils.isEmpty(request.targetCodes())) {
            throw new RuntimeException("バックアップ対象を1件以上選択してください。");
        }
    }
}