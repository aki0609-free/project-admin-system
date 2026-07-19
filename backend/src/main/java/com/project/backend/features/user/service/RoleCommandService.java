package com.project.backend.features.user.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.security.permission.entity.Permission;
import com.project.backend.app.security.permission.entity.Role;
import com.project.backend.app.security.permission.repository.PermissionRepository;
import com.project.backend.app.security.permission.repository.RoleRepository;
import com.project.backend.features.user.dto.RoleCreateRequest;
import com.project.backend.features.user.dto.RoleUpdateRequest;
import com.project.backend.features.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleCommandService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @SuppressWarnings("null")
    public Long create(RoleCreateRequest request) {
        validateRoleName(request.getName());

        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("同じロール名が既に存在します。name=" + request.getName());
        }

        Set<Permission> permissions = getPermissions(request.getPermissionIds());

        Role role = Role.builder()
                .name(request.getName())
                .permissions(permissions)
                .build();

        return roleRepository.save(role).getId();
    }

    @SuppressWarnings("null")
    public void update(Long id, RoleUpdateRequest request) {
        validateRoleName(request.getName());

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ロールが見つかりません。id=" + id));

        if (!role.getName().equals(request.getName())
                && roleRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("同じロール名が既に存在します。name=" + request.getName());
        }

        Set<Permission> permissions = getPermissions(request.getPermissionIds());

        role.setName(request.getName());
        role.setPermissions(permissions);

        roleRepository.save(role);
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ロールが見つかりません。id=" + id));

        if ("SYS_ADMIN".equals(role.getName())) {
            throw new IllegalArgumentException("SYS_ADMIN ロールは削除できません。");
        }

        if (userRepository.existsActiveUserByRoleId(id)) {
            throw new IllegalArgumentException("このロールはユーザーに使用されているため削除できません。");
        }
        // 論理削除済みユーザーも含めて user_role の紐付けを消す
        userRepository.deleteUserRoleMappingsByRoleId(id);

        // role_permission の紐付けも消す
        roleRepository.deleteRolePermissionMappingsByRoleId(id);

        // 最後に roles を物理削除
        roleRepository.delete(role);
    }

    private void validateRoleName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ロール名は必須です。");
        }
    }

    private Set<Permission> getPermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new IllegalArgumentException("権限は1件以上必要です。");
        }

        Set<Permission> permissions = permissionRepository.findByIdIn(permissionIds);

        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("存在しない権限が含まれています。permissionIds=" + permissionIds);
        }

        return permissions;
    }
}