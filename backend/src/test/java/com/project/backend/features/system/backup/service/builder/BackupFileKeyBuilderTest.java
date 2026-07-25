package com.project.backend.features.system.backup.service.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;

class BackupFileKeyBuilderTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void buildsFileManagerVisibleSystemBackupKey() {
        TenantContext.setTenantId("tenant-a");

        BackupFileKeyBuilder builder = builder();

        assertThat(builder.build(
                "master-data",
                "REPORT_20260725.csv"
        )).isEqualTo(
                "documents/backups/system/tenant-a/master-data/REPORT_20260725.csv"
        );
    }

    @Test
    void rejectsPathInFileName() {
        TenantContext.setTenantId("tenant-a");

        assertThatThrownBy(() ->
                builder().build("master-data", "../report.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("パス");
    }

    private BackupFileKeyBuilder builder() {
        StorageProperties properties = new StorageProperties();
        return new BackupFileKeyBuilder(
                new DocumentStorageKeyResolver(properties)
        );
    }
}
