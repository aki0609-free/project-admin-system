package com.project.backend.features.user.dto;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    private String username;
    private String password;
    private Boolean enabled;
    private Set<String> roles;
}