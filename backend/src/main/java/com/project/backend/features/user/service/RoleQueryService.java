package com.project.backend.features.user.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.security.permission.entity.Permission;
import com.project.backend.app.security.permission.entity.Role;
import com.project.backend.app.security.permission.repository.RoleRepository;
import com.project.backend.features.user.dto.PermissionResponse;
import com.project.backend.features.user.dto.RoleDetailResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleQueryService {

    private final RoleRepository roleRepository;

    public List<RoleDetailResponse> findAll() {
        return roleRepository.findAllWithPermissions().stream()
                .map(this::toRoleDetailResponse)
                .toList();
    }

    private RoleDetailResponse toRoleDetailResponse(Role role) {
        return RoleDetailResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .permissions(
                        role.getPermissions().stream()
                                .map(this::toPermissionResponse)
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .build();
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .label(permission.getLabel())
                .build();
    }
}