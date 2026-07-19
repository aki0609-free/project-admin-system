package com.project.backend.features.system.batch.service.executor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.batch.enums.BatchJobType;

@Component
public class BatchJobExecutorResolver {

    private final Map<BatchJobType, BatchJobExecutor> executorMap =
            new EnumMap<>(BatchJobType.class);

    public BatchJobExecutorResolver(List<BatchJobExecutor> executors) {
        for (BatchJobExecutor executor : executors) {
            executorMap.put(executor.supportType(), executor);
        }
    }

    public BatchJobExecutor resolve(BatchJobType jobType) {
        BatchJobExecutor executor = executorMap.get(jobType);

        if (executor == null) {
            throw new RuntimeException("未対応のjobTypeです。 jobType=" + jobType);
        }

        return executor;
    }
}