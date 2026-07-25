package com.project.backend.features.system.batch.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.batch.dto.BatchJobDefinitionSaveRequest;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.batch.enums.BatchScheduleType;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.app.tenant.context.TenantContext;

class BatchJobDefinitionValidatorTest {

    private BatchJobDefinitionRepository repository;
    private BatchJobDefinitionValidator validator;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("default");
        repository = mock(BatchJobDefinitionRepository.class);
        validator = new BatchJobDefinitionValidator(repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void invalidCronIsRejected() {
        assertThatThrownBy(() ->
                validator.validate(request("REPORT_MONTHLY", "invalid"), null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cronExpression");
    }

    @Test
    void jobCodeCannotBeChangedAfterCreation() {
        BatchJobDefinition existing = new BatchJobDefinition();
        existing.setId(1L);
        existing.setJobCode("REPORT_MONTHLY");
        when(repository.findByIdAndTenantIdAndDeletedAtIsNull(1L, "default"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                validator.validate(request("REPORT_RENAMED", "0 0 9 * * *"), 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("変更できません");
    }

    @Test
    void lowerCaseJobCodeIsRejected() {
        assertThatThrownBy(() ->
                validator.validate(request("report_monthly", "0 0 9 * * *"), null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("英大文字");
    }

    private BatchJobDefinitionSaveRequest request(
            String jobCode,
            String cronExpression
    ) {
        return new BatchJobDefinitionSaveRequest(
                jobCode,
                "月次帳票",
                BatchJobType.REPORT,
                "MONTHLY_REPORT",
                true,
                true,
                BatchScheduleType.CRON,
                cronExpression,
                true,
                null
        );
    }
}
