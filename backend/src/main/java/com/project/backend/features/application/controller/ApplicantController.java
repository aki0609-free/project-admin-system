package com.project.backend.features.application.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.application.dto.ApplicantCreateRequest;
import com.project.backend.features.application.dto.ApplicantDetailResponse;
import com.project.backend.features.application.dto.ApplicantListItemResponse;
import com.project.backend.features.application.dto.ApplicantUpdateRequest;
import com.project.backend.features.application.service.ApplicantCommandService;
import com.project.backend.features.application.service.ApplicantQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/applicants")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantQueryService applicantQueryService;
    private final ApplicantCommandService applicantCommandService;

    @GetMapping
    public List<ApplicantListItemResponse> findAll() {
        return applicantQueryService.findAll();
    }

    @GetMapping("/{id}")
    public ApplicantDetailResponse findDetail(@PathVariable Long id) {
        return applicantQueryService.findDetail(id);
    }

    @PostMapping
    public Long create(@RequestBody ApplicantCreateRequest request) {
        return applicantCommandService.create(request);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody ApplicantUpdateRequest request
    ) {
        applicantCommandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        applicantCommandService.delete(id);
    }
}