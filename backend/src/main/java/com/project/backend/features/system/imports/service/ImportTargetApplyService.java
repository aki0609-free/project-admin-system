package com.project.backend.features.system.imports.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.imports.dto.ImportColumnSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.entity.ImportColumn;
import com.project.backend.features.system.imports.entity.ImportTarget;
import com.project.backend.features.system.imports.mapper.ImportTargetMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTargetApplyService {

    private final ImportTargetMapper mapper;

    public void apply(ImportTarget entity, ImportTargetSaveRequest request) {
        mapper.updateTargetFromRequest(request, entity);

        entity.clearColumns();

        List<ImportColumnSaveRequest> columns = request.columns() != null
                ? request.columns().stream()
                        .sorted(Comparator.comparingInt(ImportColumnSaveRequest::orderNo))
                        .toList()
                : List.of();

        for (ImportColumnSaveRequest columnRequest : columns) {
            ImportColumn column = new ImportColumn();
            mapper.updateColumnFromRequest(columnRequest, column);
            entity.addColumn(column);
        }
    }
}