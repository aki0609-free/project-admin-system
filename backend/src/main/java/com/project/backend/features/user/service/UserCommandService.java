package com.project.backend.features.user.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.security.permission.entity.Role;
import com.project.backend.app.security.permission.repository.RoleRepository;
import com.project.backend.features.user.dto.UserCreateRequest;
import com.project.backend.features.user.dto.UserUpdateRequest;
import com.project.backend.features.user.entity.User;
import com.project.backend.features.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @SuppressWarnings("null")
    public Long create(UserCreateRequest request) {
        validateDuplicateUsername(request.getUsername());

        Set<Role> roles = getRoles(request.getRoles());

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(request.getEnabled())
                .roles(roles)
                .build();

        return userRepository.save(user).getId();
    }

    @SuppressWarnings("null")
    public void update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません。id=" + id));

        if (!user.getUsername().equals(request.getUsername())) {
            validateDuplicateUsername(request.getUsername());
        }

        Set<Role> roles = getRoles(request.getRoles());

        user.setUsername(request.getUsername());
        user.setEnabled(request.getEnabled());
        user.setRoles(roles);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません。id=" + id));

        String deletedUsername = buildDeletedUsername(user.getUsername(), user.getId());

        user.setUsername(deletedUsername);
        user.setEnabled(false);
        user.setDeletedAt(Instant.now());

        userRepository.save(user);
    }

    private String buildDeletedUsername(String username, Long id) {
        String suffix = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        return username + "__deleted__" + id + "__" + suffix;
    }

    private void validateDuplicateUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("同じユーザー名が既に存在します。username=" + username);
        }
    }

    private Set<Role> getRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("ロールは1件以上必要です。");
        }

        Set<Role> roles = roleRepository.findByNameIn(roleNames);

        if (roles.size() != roleNames.size()) {
            throw new IllegalArgumentException("存在しないロールが含まれています。roles=" + roleNames);
        }

        return roles;
    }
}