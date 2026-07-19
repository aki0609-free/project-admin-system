package com.project.backend.features.user.dto;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleDetailResponse {
    private Long id;
    private String name;
    private Set<PermissionResponse> permissions;
}