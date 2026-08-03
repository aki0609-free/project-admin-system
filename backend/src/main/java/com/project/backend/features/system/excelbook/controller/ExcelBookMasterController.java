package com.project.backend.features.system.excelbook.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterRequest;
import com.project.backend.features.system.excelbook.dto.ExcelBookMasterResponse;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateResponse;
import com.project.backend.features.system.excelbook.dto.SpreadsheetTemplateSaveRequest;
import com.project.backend.features.system.excelbook.service.ExcelBookMasterCommandService;
import com.project.backend.features.system.excelbook.service.ExcelBookMasterQueryService;
import com.project.backend.features.system.excelbook.service.SpreadsheetTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/excel-book-masters")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class ExcelBookMasterController {

    private final ExcelBookMasterQueryService queryService;
    private final ExcelBookMasterCommandService commandService;
    private final SpreadsheetTemplateService spreadsheetTemplateService;

    @GetMapping
    public List<ExcelBookMasterResponse> findAll() {
        return queryService.findAll();
    }

    @GetMapping("/{id}")
    public ExcelBookMasterResponse findById(@PathVariable Long id) {
        return queryService.findById(id);
    }

    @PostMapping
    public Long create(@RequestBody ExcelBookMasterRequest request) {
        return commandService.create(request);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody ExcelBookMasterRequest request
    ) {
        commandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        commandService.delete(id);
    }

    @GetMapping("/{id}/spreadsheet-template")
    public SpreadsheetTemplateResponse findSpreadsheetTemplate(
            @PathVariable Long id
    ) {
        return spreadsheetTemplateService.find(id);
    }

    @PutMapping("/{id}/spreadsheet-template")
    public SpreadsheetTemplateResponse saveSpreadsheetTemplate(
            @PathVariable Long id,
            @RequestBody SpreadsheetTemplateSaveRequest request
    ) {
        return spreadsheetTemplateService.save(id, request);
    }
}
