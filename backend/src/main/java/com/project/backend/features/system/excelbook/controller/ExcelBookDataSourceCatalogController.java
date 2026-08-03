package com.project.backend.features.system.excelbook.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.excelbook.dto.ExcelBookDataSourceCatalogResponse;
import com.project.backend.features.system.excelbook.service.ExcelBookDataSourceCatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/excel-book-data-source-catalogs")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class ExcelBookDataSourceCatalogController {

    private final ExcelBookDataSourceCatalogService service;

    @GetMapping("/active")
    public List<ExcelBookDataSourceCatalogResponse> findActive() {
        return service.findActive();
    }
}
