package com.project.backend.features.system.backup.service.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupFileKeyBuilder {

    private final DocumentStorageKeyResolver documentStorageKeyResolver;

    public String build(
            String outputDir,
            String fileName
    ) {
        validateFileName(fileName);

        String tenantId = requireTenantId();
        String relativePath = StringUtils.hasText(outputDir)
                ? "system/" + tenantId + "/" + outputDir + "/" + fileName
                : "system/" + tenantId + "/" + fileName;

        return documentStorageKeyResolver.resolve(
                DocumentArea.BACKUPS,
                relativePath
        );
    }

    private void validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new RuntimeException("バックアップファイル名が未設定です。");
        }
        if (fileName.contains("/") || fileName.contains("\\")) {
            throw new RuntimeException(
                    "バックアップファイル名にパスを含めることはできません。"
            );
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new RuntimeException("テナント情報を取得できません。");
        }
        return tenantId;
    }
}
