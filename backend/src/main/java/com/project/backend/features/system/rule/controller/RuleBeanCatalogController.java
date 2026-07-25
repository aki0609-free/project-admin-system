package com.project.backend.features.system.rule.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.rule.service.RuleBeanCatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/rule-beans")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class RuleBeanCatalogController {

    private final RuleBeanCatalogService service;

    @GetMapping
    public List<String> findBeanNames() {
        return service.findBeanNames();
    }
}
