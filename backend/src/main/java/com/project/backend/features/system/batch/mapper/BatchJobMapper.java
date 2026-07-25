package com.project.backend.features.system.batch.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.project.backend.features.system.batch.dto.BatchExecutionLogResponse;
import com.project.backend.features.system.batch.dto.BatchJobDefinitionResponse;
import com.project.backend.features.system.batch.dto.BatchJobDefinitionSaveRequest;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchScheduleType;

@Mapper(
        componentModel = "spring",
        imports = BatchScheduleType.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BatchJobMapper {

    BatchJobDefinitionResponse toDefinitionResponse(
            BatchJobDefinition entity
    );

    List<BatchJobDefinitionResponse> toDefinitionResponseList(
            List<BatchJobDefinition> entities
    );

    @Mapping(
            target = "immediateExecutable",
            expression = "java(request.immediateExecutable() != null ? request.immediateExecutable() : true)"
    )
    @Mapping(
            target = "scheduleEnabled",
            expression = "java(request.scheduleEnabled() != null ? request.scheduleEnabled() : false)"
    )
    @Mapping(
            target = "scheduleType",
            expression = "java(Boolean.TRUE.equals(request.scheduleEnabled()) && request.scheduleType() != null ? request.scheduleType() : BatchScheduleType.NONE)"
    )
    @Mapping(
            target = "cronExpression",
            expression = "java(Boolean.TRUE.equals(request.scheduleEnabled()) && request.scheduleType() == BatchScheduleType.CRON ? request.cronExpression() : null)"
    )
    @Mapping(
            target = "activeFlag",
            expression = "java(request.activeFlag() != null ? request.activeFlag() : true)"
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "lastExecutedAt", ignore = true)
    @Mapping(target = "nextExecuteAt", ignore = true)
    void updateDefinitionFromRequest(
            BatchJobDefinitionSaveRequest request,
            @MappingTarget BatchJobDefinition entity
    );

    BatchExecutionLogResponse toLogResponse(
            BatchExecutionLog entity
    );

    List<BatchExecutionLogResponse> toLogResponseList(
            List<BatchExecutionLog> entities
    );
}
