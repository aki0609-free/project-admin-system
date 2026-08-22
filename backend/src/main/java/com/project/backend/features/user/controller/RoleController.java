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
import org.springframework.security.access.prepost.PreAuthorize;

import com.project.backend.features.user.dto.RoleCreateRequest;
import com.project.backend.features.user.dto.RoleDetailResponse;
import com.project.backend.features.user.dto.RoleUpdateRequest;
import com.project.backend.features.user.service.RoleCommandService;
import com.project.backend.features.user.service.RoleQueryService;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:view')")
public class RoleController {

    private final RoleQueryService roleQueryService;
    private final RoleCommandService roleCommandService;

    @GetMapping
    public List<RoleDetailResponse> findAll() {
        return roleQueryService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public Long create(@Valid @RequestBody RoleCreateRequest request) {
        return roleCommandService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public void update(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        roleCommandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public void delete(@PathVariable Long id) {
        roleCommandService.delete(id);
    }
}
