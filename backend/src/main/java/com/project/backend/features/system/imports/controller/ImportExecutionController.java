package com.project.backend.features.system.imports.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.features.system.imports.dto.ImportExecuteResult;
import com.project.backend.features.system.imports.service.ImportExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/import")
@RequiredArgsConstructor
public class ImportExecutionController {

    private final ImportExecutionService importExecutionService;

    @PostMapping("/execute")
    public ImportExecuteResult executeUpload(
            @RequestParam String targetCode,
            @RequestParam MultipartFile file
    ) {
        return importExecutionService.executeUpload(targetCode, file);
    }

    @PostMapping("/execute-defined")
    public ImportExecuteResult executeDefined(
            @RequestParam String targetCode
    ) {
        return importExecutionService.executeFromDefinition(targetCode);
    }
}