package com.project.backend.features.user.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.persistence.EnableHibernateFilters;
import com.project.backend.app.security.permission.entity.Permission;
import com.project.backend.app.security.permission.entity.Role;
import com.project.backend.features.user.dto.PermissionResponse;
import com.project.backend.features.user.dto.RoleDetailResponse;
import com.project.backend.features.user.dto.UserDetailResponse;
import com.project.backend.features.user.dto.UserListItemResponse;
import com.project.backend.features.user.entity.User;
import com.project.backend.features.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@EnableHibernateFilters
public class UserQueryService {

    private final UserRepository userRepository;

    public List<UserListItemResponse> findAll() {
        return userRepository.findAllByOrderByIdAsc().stream()
                .map(this::toListItemResponse)
                .toList();
    }

    public UserDetailResponse findDetail(Long id) {
        User user = userRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません。id=" + id));

        return toDetailResponse(user);
    }

    private UserListItemResponse toListItemResponse(User user) {
        return UserListItemResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .enabled(user.getEnabled())
                .roles(
                    user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .build();
    }

    private UserDetailResponse toDetailResponse(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .enabled(user.getEnabled())
                .roles(
                    user.getRoles().stream()
                        .map(this::toRoleDetailResponse)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .build();
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
                .build();
    }
}