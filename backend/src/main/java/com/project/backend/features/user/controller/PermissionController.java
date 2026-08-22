package com.project.backend.features.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.project.backend.features.user.dto.PermissionResponse;
import com.project.backend.features.user.service.PermissionQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:view')")
public class PermissionController {

    private final PermissionQueryService permissionQueryService;

    @GetMapping
    public List<PermissionResponse> findAll() {
        return permissionQueryService.findAll();
    }
}
