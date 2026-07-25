package com.project.backend.features.system.rule.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.rule.dto.RuleDataSourceCatalogResponse;
import com.project.backend.features.system.rule.service.RuleDataSourceCatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/rule-data-source-catalogs")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class RuleDataSourceCatalogController {

    private final RuleDataSourceCatalogService service;

    @GetMapping("/active")
    public List<RuleDataSourceCatalogResponse> findActive() {
        return service.findActive();
    }
}
