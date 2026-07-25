package com.project.backend.features.system.backup.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.HashSet;

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
        if (request.targetCodes().size() > 50) {
            throw new RuntimeException("バックアップ対象は50件以内で選択してください。");
        }
        if (request.targetCodes().stream()
                .anyMatch(code -> code == null || code.isBlank())) {
            throw new RuntimeException("空のバックアップ対象は指定できません。");
        }
        if (request.targetCodes().stream().anyMatch(code -> code.length() > 100)) {
            throw new RuntimeException("バックアップ対象コードは100文字以内です。");
        }
        if (String.join(",", request.targetCodes()).length() > 2000) {
            throw new RuntimeException("バックアップ対象コードの合計が長すぎます。");
        }
        if (new HashSet<>(request.targetCodes()).size()
                != request.targetCodes().size()) {
            throw new RuntimeException("バックアップ対象が重複しています。");
        }
    }
}
