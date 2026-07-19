package com.project.backend.features.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.user.dto.RoleCreateRequest;
import com.project.backend.features.user.dto.RoleDetailResponse;
import com.project.backend.features.user.dto.RoleUpdateRequest;
import com.project.backend.features.user.service.RoleCommandService;
import com.project.backend.features.user.service.RoleQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleQueryService roleQueryService;
    private final RoleCommandService roleCommandService;

    @GetMapping
    public List<RoleDetailResponse> findAll() {
        return roleQueryService.findAll();
    }

    @PostMapping
    public Long create(@RequestBody RoleCreateRequest request) {
        return roleCommandService.create(request);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request
    ) {
        roleCommandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleCommandService.delete(id);
    }
}