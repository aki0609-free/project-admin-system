package com.project.backend.app.security.auth.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUserDto {

    private Long id;

    private String tenantId;

    private String username;

    private Set<String> roles;

    private Set<String> permissions;
    
}
