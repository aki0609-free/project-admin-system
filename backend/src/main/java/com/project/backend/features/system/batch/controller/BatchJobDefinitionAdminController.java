package com.project.backend.features.system.batch.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.batch.dto.BatchJobDefinitionResponse;
import com.project.backend.features.system.batch.dto.BatchJobDefinitionSaveRequest;
import com.project.backend.features.system.batch.service.BatchJobDefinitionCommandService;
import com.project.backend.features.system.batch.service.BatchJobDefinitionQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/batch-jobs")
@RequiredArgsConstructor
public class BatchJobDefinitionAdminController {

    private final BatchJobDefinitionQueryService queryService;
    private final BatchJobDefinitionCommandService commandService;

    @GetMapping
    public List<BatchJobDefinitionResponse> findAll() {
        return queryService.findAll();
    }

    @GetMapping("/{id}")
    public BatchJobDefinitionResponse findDetail(
            @PathVariable Long id
    ) {
        return queryService.findDetail(id);
    }

    @PostMapping
    public BatchJobDefinitionResponse create(
            @RequestBody BatchJobDefinitionSaveRequest request
    ) {
        return commandService.create(request);
    }

    @PutMapping("/{id}")
    public BatchJobDefinitionResponse update(
            @PathVariable Long id,
            @RequestBody BatchJobDefinitionSaveRequest request
    ) {
        return commandService.update(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        commandService.delete(id);
    }
}