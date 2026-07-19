package com.project.backend.features.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.security.permission.repository.PermissionRepository;
import com.project.backend.features.user.dto.PermissionResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionQueryService {

    private final PermissionRepository permissionRepository;

    public List<PermissionResponse> findAll() {
        return permissionRepository.findAll().stream()
                .map(permission -> PermissionResponse.builder()
                        .id(permission.getId())
                        .name(permission.getName())
                        .label(permission.getLabel())
                        .build())
                .toList();
    }
}
