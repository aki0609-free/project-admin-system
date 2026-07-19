package com.project.backend.features.system.batch.service.validation;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.batch.dto.BatchJobDefinitionSaveRequest;
import com.project.backend.features.system.batch.enums.BatchScheduleType;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchJobDefinitionValidator {

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

        if (!StringUtils.hasText(request.jobName())) {
            throw new RuntimeException("jobName は必須です。");
        }

        if (request.jobType() == null) {
            throw new RuntimeException("jobType は必須です。");
        }

        if (!StringUtils.hasText(request.targetCode())) {
            throw new RuntimeException("targetCode は必須です。");
        }

        if (Boolean.TRUE.equals(request.scheduleEnabled())) {

            if (request.scheduleType() == null
                    || request.scheduleType() == BatchScheduleType.NONE) {
                throw new RuntimeException(
                        "scheduleEnabled=true の場合、scheduleType は必須です。"
                );
            }

            if (request.scheduleType() == BatchScheduleType.CRON
                    && !StringUtils.hasText(request.cronExpression())) {
                throw new RuntimeException(
                        "scheduleType=CRON の場合、cronExpression は必須です。"
                );
            }
        }

        boolean exists = id == null
                ? repository.existsByJobCodeAndDeletedAtIsNull(
                        request.jobCode()
                )
                : repository.existsByJobCodeAndIdNotAndDeletedAtIsNull(
                        request.jobCode(),
                        id
                );

        if (exists) {
            throw new RuntimeException(
                    "jobCode が重複しています。 jobCode=" + request.jobCode()
            );
        }
    }
}