package com.project.backend.features.system.imports.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;

class ImportSqlBuilderTest {

    private final ImportSqlBuilder builder =
            new ImportSqlBuilder();

    @Test
    void テナント対象のinsertへ監査列を追加する() {
        String sql = builder.buildInsertSql(
                "resident_tax_monthly",
                List.of(column("employee_id", true)),
                true
        );

        assertThat(sql)
                .contains(
                        "tenant_id",
                        "created_at",
                        "updated_at",
                        ":__tenantId",
                        ":__now"
                );
    }

    @Test
    void テナント対象のupdateとexistsをtenantIdで限定する() {
        ImportColumnDefinition key =
                column("employee_id", true);
        ImportColumnDefinition value =
                column("tax_amount", false);

        String updateSql = builder.buildUpdateSql(
                "resident_tax_monthly",
                List.of(key),
                List.of(value),
                true
        );
        String existsSql = builder.buildExistsSql(
                "resident_tax_monthly",
                List.of(key),
                true
        );

        assertThat(updateSql)
                .contains("tenant_id = :__tenantId")
                .contains("updated_at = :__now");
        assertThat(existsSql)
                .contains("tenant_id = :__tenantId");
    }

    @Test
    void deleteInsertはテナント対象だけを削除できる() {
        assertThat(
                builder.buildDeleteSql(
                        "resident_tax_monthly",
                        true
                )
        ).isEqualTo(
                "DELETE FROM resident_tax_monthly WHERE tenant_id = :tenantId"
        );
    }

    private ImportColumnDefinition column(
            String name,
            boolean key
    ) {
        return ImportColumnDefinition.builder()
                .columnName(name)
                .csvHeaderName(name)
                .keyFlag(key)
                .updatableFlag(!key)
                .orderNo(1)
                .build();
    }
}
