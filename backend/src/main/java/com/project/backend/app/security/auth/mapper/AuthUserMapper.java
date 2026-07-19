package com.project.backend.app.security.auth.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.project.backend.features.user.entity.User;
import com.project.backend.app.security.auth.dto.AuthUserDto;
import com.project.backend.app.security.permission.entity.Permission;
import com.project.backend.app.security.permission.entity.Role;

@Mapper(componentModel = "spring")
public interface AuthUserMapper {

    @Mapping(
        target = "roles",
        expression = "java(mapRoles(user.getRoles()))"
    )
    @Mapping(
        target = "permissions",
        expression = "java(mapPermissions(user.getRoles()))"
    )
    AuthUserDto toAuthUserDto(User user);

    // roles
    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) return Set.of();

        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    // permissions
    default Set<String> mapPermissions(Set<Role> roles) {
        if (roles == null) return Set.of();

        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}
