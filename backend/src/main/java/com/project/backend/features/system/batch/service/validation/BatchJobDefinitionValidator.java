package com.project.backend.features.system.batch.service.validation;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;
import java.util.regex.Pattern;

import com.project.backend.features.system.batch.dto.BatchJobDefinitionSaveRequest;
import com.project.backend.features.system.batch.enums.BatchScheduleType;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchJobDefinitionValidator {

    private static final Pattern JOB_CODE_PATTERN =
            Pattern.compile("[A-Z][A-Z0-9_]{1,99}");

    private final BatchJobDefinitionRepository repository;

    public void validate(
            BatchJobDefinitionSaveRequest request,
            Long id
    ) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.jobCode())) {
            throw new RuntimeException("jobCode は必須です。");
        }
        if (!JOB_CODE_PATTERN.matcher(request.jobCode()).matches()) {
            throw new RuntimeException(
                    "jobCode は英大文字で始まる英大文字・数字・アンダースコアで指定してください。"
            );
        }

        if (!StringUtils.hasText(request.jobName())) {
            throw new RuntimeException("jobName は必須です。");
        }
        if (request.jobName().length() > 200) {
            throw new RuntimeException("jobName は200文字以内です。");
        }

        if (request.jobType() == null) {
            throw new RuntimeException("jobType は必須です。");
        }

        if (!StringUtils.hasText(request.targetCode())) {
            throw new RuntimeException("targetCode は必須です。");
        }
        if (request.targetCode().length() > 100) {
            throw new RuntimeException("targetCode は100文字以内です。");
        }
        if (request.description() != null && request.description().length() > 500) {
            throw new RuntimeException("description は500文字以内です。");
        }

        if (Boolean.TRUE.equals(request.scheduleEnabled())) {

            if (request.scheduleType() == null
                    || request.scheduleType() == BatchScheduleType.NONE) {
                throw new RuntimeException(
                        "scheduleEnabled=true の場合、scheduleType は必須です。"
                );
            }

            if (request.scheduleType() == BatchScheduleType.CRON
                    ) {
                if (!StringUtils.hasText(request.cronExpression())) {
                    throw new RuntimeException(
                            "scheduleType=CRON の場合、cronExpression は必須です。"
                    );
                }
                if (!CronExpression.isValidExpression(request.cronExpression())) {
                    throw new RuntimeException("cronExpression の形式が不正です。");
                }
                if (request.cronExpression().length() > 100) {
                    throw new RuntimeException("cronExpression は100文字以内です。");
                }
            }
        }

        if (id != null) {
            repository.findByIdAndTenantIdAndDeletedAtIsNull(
                    id,
                    requireTenantId()
            ).ifPresent(existing -> {
                if (!existing.getJobCode().equals(request.jobCode())) {
                    throw new RuntimeException(
                            "作成後のjobCodeは変更できません。 jobCode="
                                    + existing.getJobCode()
                    );
                }
            });
        }

        boolean exists = id == null
                ? repository.existsByTenantIdAndJobCodeAndDeletedAtIsNull(
                        requireTenantId(),
                        request.jobCode()
                )
                : repository.existsByTenantIdAndJobCodeAndIdNotAndDeletedAtIsNull(
                        requireTenantId(),
                        request.jobCode(),
                        id
                );

        if (exists) {
            throw new RuntimeException(
                    "jobCode が重複しています。 jobCode=" + request.jobCode()
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
