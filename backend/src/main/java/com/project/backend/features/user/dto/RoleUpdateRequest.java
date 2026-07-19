package com.project.backend.features.user.dto;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    private String name;
    private Set<Long> permissionIds;
}
