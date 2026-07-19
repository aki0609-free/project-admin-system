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

import com.project.backend.features.application.dto.ApplicationMediaBulkSaveRequest;
import com.project.backend.features.application.dto.ApplicationMediaCreateRequest;
import com.project.backend.features.application.dto.ApplicationMediaDetailResponse;
import com.project.backend.features.application.dto.ApplicationMediaListItemResponse;
import com.project.backend.features.application.dto.ApplicationMediaUpdateRequest;
import com.project.backend.features.application.service.ApplicationMediaCommandService;
import com.project.backend.features.application.service.ApplicationMediaQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/application-media")
@RequiredArgsConstructor
public class ApplicationMediaController {

    private final ApplicationMediaQueryService applicationMediaQueryService;
    private final ApplicationMediaCommandService applicationMediaCommandService;

    @GetMapping
    public List<ApplicationMediaListItemResponse> findAll() {
        return applicationMediaQueryService.findAll();
    }

    @GetMapping("/{id}")
    public ApplicationMediaDetailResponse findDetail(@PathVariable Long id) {
        return applicationMediaQueryService.findDetail(id);
    }

    @PostMapping
    public Long create(@RequestBody ApplicationMediaCreateRequest request) {
        return applicationMediaCommandService.create(request);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody ApplicationMediaUpdateRequest request
    ) {
        applicationMediaCommandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        applicationMediaCommandService.delete(id);
    }

    @PostMapping("/bulk-save")
    public void bulkSave(@RequestBody ApplicationMediaBulkSaveRequest request) {
        applicationMediaCommandService.bulkSave(request);
    }
}