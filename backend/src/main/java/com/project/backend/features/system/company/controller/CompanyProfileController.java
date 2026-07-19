package com.project.backend.features.system.company.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.company.dto.CompanyProfileResponse;
import com.project.backend.features.system.company.dto.CompanyProfileSaveRequest;
import com.project.backend.features.system.company.service.CompanyProfileCommandService;
import com.project.backend.features.system.company.service.CompanyProfileQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/company-profile")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileQueryService queryService;
    private final CompanyProfileCommandService commandService;

    @GetMapping
    public CompanyProfileResponse findCurrent() {
        return queryService.findCurrentOrNull();
    }

    @PutMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public CompanyProfileResponse save(
            @RequestBody CompanyProfileSaveRequest request
    ) {
        return commandService.save(request);
    }
}