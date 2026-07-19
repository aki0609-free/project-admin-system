package com.project.backend.features.user.dto;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDetailResponse {
    private Long id;
    private String username;
    private Boolean enabled;
    private Set<RoleDetailResponse> roles;
}