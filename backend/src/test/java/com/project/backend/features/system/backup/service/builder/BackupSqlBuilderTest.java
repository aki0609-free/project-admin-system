package com.project.backend.features.system.backup.service.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.backup.dto.BackupColumnDefinition;
import com.project.backend.features.system.backup.enums.BackupDataType;

class BackupSqlBuilderTest {

    private final BackupSqlBuilder builder = new BackupSqlBuilder();

    @Test
    void tenantScopedTableForcesTenantCondition() {
        String sql = builder.buildSelectSql(
                "report_master",
                List.of(column("report_code")),
                true
        );

        assertThat(sql).isEqualTo(
                "SELECT report_code FROM report_master WHERE tenant_id = :tenantId"
        );
        assertThat(builder.buildParameters(true, "tenant-a"))
                .containsEntry("tenantId", "tenant-a");
    }

    @Test
    void globalTableDoesNotAddTenantCondition() {
        String sql = builder.buildSelectSql(
                "global_master",
                List.of(column("code")),
                false
        );

        assertThat(sql).isEqualTo("SELECT code FROM global_master");
        assertThat(builder.buildParameters(false, null)).isEmpty();
    }

    @Test
    void unsafeIdentifierIsRejected() {
        assertThatThrownBy(() ->
                builder.buildSelectSql(
                        "users; DELETE FROM users",
                        List.of(column("id")),
                        true
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tableName");
    }

    private BackupColumnDefinition column(String name) {
        return BackupColumnDefinition.builder()
                .columnName(name)
                .csvHeaderName(name)
                .dataType(BackupDataType.STRING)
                .exportFlag(true)
                .orderNo(1)
                .build();
    }
}
