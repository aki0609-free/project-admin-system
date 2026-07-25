package com.project.backend.features.system.backup.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.backup.dto.BackupColumnSaveRequest;
import com.project.backend.features.system.backup.dto.BackupSourceSchema;
import com.project.backend.features.system.backup.dto.BackupTargetSaveRequest;
import com.project.backend.features.system.backup.entity.BackupTarget;
import com.project.backend.features.system.backup.enums.BackupDataType;
import com.project.backend.features.system.backup.enums.BackupOutputMode;
import com.project.backend.features.system.backup.repository.BackupTargetRepository;

class BackupTargetValidatorTest {

    private BackupTargetRepository repository;
    private BackupSchemaInspector schemaInspector;
    private BackupTargetValidator validator;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
        repository = mock(BackupTargetRepository.class);
        schemaInspector = mock(BackupSchemaInspector.class);
        validator = new BackupTargetValidator(repository, schemaInspector);

        when(schemaInspector.inspect("report_master"))
                .thenReturn(new BackupSourceSchema(
                        "report_master",
                        Set.of("report_code", "tenant_id"),
                        true
                ));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void missingPhysicalColumnIsRejected() {
        assertThatThrownBy(() ->
                validator.validate(request("missing_column"), null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("存在しないカラム");
    }

    @Test
    void targetCodeAndTableCannotBeChanged() {
        BackupTarget existing = new BackupTarget();
        existing.setTargetCode("BACKUP_REPORT_MASTER");
        existing.setTableName("report_master");

        when(repository.findByIdAndTenantIdAndDeletedAtIsNull(
                1L,
                "tenant-a"
        )).thenReturn(Optional.of(existing));

        BackupTargetSaveRequest renamed = new BackupTargetSaveRequest(
                "BACKUP_REPORT_RENAMED",
                "帳票マスタ",
                "report_master",
                null,
                BackupOutputMode.DOWNLOAD,
                null,
                "{targetCode}_{timestamp}.csv",
                false,
                true,
                true,
                true,
                request("report_code").columns()
        );

        assertThatThrownBy(() -> validator.validate(renamed, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("targetCode");
    }

    @Test
    void fileNamePatternRequiresTimestamp() {
        BackupTargetSaveRequest request = new BackupTargetSaveRequest(
                "BACKUP_REPORT_MASTER",
                "帳票マスタ",
                "report_master",
                null,
                BackupOutputMode.DOWNLOAD,
                null,
                "report.csv",
                false,
                true,
                true,
                true,
                request("report_code").columns()
        );

        assertThatThrownBy(() -> validator.validate(request, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("timestamp");
    }

    private BackupTargetSaveRequest request(String columnName) {
        return new BackupTargetSaveRequest(
                "BACKUP_REPORT_MASTER",
                "帳票マスタ",
                "report_master",
                null,
                BackupOutputMode.DOWNLOAD,
                null,
                "{targetCode}_{timestamp}.csv",
                false,
                true,
                true,
                true,
                List.of(new BackupColumnSaveRequest(
                        null,
                        columnName,
                        "帳票コード",
                        BackupDataType.STRING,
                        true,
                        1
                ))
        );
    }
}
