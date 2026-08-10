package com.project.backend.app.config;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.project.backend.app.security.permission.entity.Permission;
import com.project.backend.app.security.permission.entity.Role;
import com.project.backend.app.security.permission.repository.PermissionRepository;
import com.project.backend.app.security.permission.repository.RoleRepository;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.user.entity.User;
import com.project.backend.features.user.repository.UserRepository;

import jakarta.transaction.Transactional;

@Configuration
public class InitConfig {

        @SuppressWarnings("null")
        @Bean
        @Transactional
        CommandLineRunner initSecurity(
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        PermissionRepository permissionRepository,
                        PasswordEncoder encoder,
                        Environment environment) {
                return args -> {

                        // 1. Permission定義（menuItems に対応）
                        Map<String, String> permissionMap = Map.ofEntries(
                                        Map.entry("dashboard:view", "ダッシュボード / 閲覧"),
                                        Map.entry("customer:view", "顧客管理 / 閲覧"),
                                        Map.entry("master:view", "マスター管理 / 閲覧"),
                                        Map.entry("master:manage", "マスター管理 / 管理"),
                                        Map.entry("application:view", "HR分析 / 閲覧"),
                                        Map.entry("employee:view", "従業員管理 / 閲覧"),
                                        Map.entry("operation:view", "書類管理 / 閲覧"),
                                        Map.entry("admin:view", "管理者 / 閲覧"),
                                        Map.entry("admin:manage", "管理者 / 管理"),
                                        Map.entry("user:manage", "ユーザ管理 / 管理"),
                                        Map.entry("system:manage", "システム運用 / 管理"));

                        // 2. DB投入（なければ作成、あればlabel更新）
                        Set<Permission> allPermissions = permissionMap.entrySet().stream()
                                        .map(entry -> {
                                                String name = entry.getKey();
                                                String label = entry.getValue();

                                                return permissionRepository.findByName(name)
                                                                .map(existing -> {
                                                                        // ★ labelが変わってたら更新（重要）
                                                                        if (!label.equals(existing.getLabel())) {
                                                                                existing.setLabel(label);
                                                                                return permissionRepository
                                                                                                .save(existing);
                                                                        }
                                                                        return existing;
                                                                })
                                                                .orElseGet(() -> permissionRepository.save(
                                                                                Permission.builder()
                                                                                                .name(name)
                                                                                                .label(label)
                                                                                                .build()));
                                        })
                                        .collect(Collectors.toSet());

                        // 3.SYS_ADMIN Role
                        Role sysAdminRole = roleRepository.findByName("SYS_ADMIN")
                                        .orElseGet(() -> roleRepository.save(
                                                        Role.builder()
                                                                        .name("SYS_ADMIN")
                                                                        .permissions(allPermissions)
                                                                        .build()));

                        // 4.既存でも権限を上書き
                        sysAdminRole.setPermissions(allPermissions);
                        roleRepository.save(sysAdminRole);

                        // 5.adminユーザー
                        TenantContext.setTenantId("default");
                        if (userRepository.findByUsername("akiLes").isEmpty()) {
                                User admin = User.builder()
                                                .username("akiLes")
                                                .password(encoder.encode("admin123"))
                                                .enabled(true)
                                                .roles(Set.of(sysAdminRole))
                                                .build();

                                userRepository.save(admin);

                                System.out.println("Admin user created");
                        }

                        initializeLocalE2eUser(
                                        userRepository,
                                        sysAdminRole,
                                        encoder,
                                        environment);
                        TenantContext.clear();

                        System.out.println("Permissions initialized");
                };
        }

        private void initializeLocalE2eUser(
                        UserRepository userRepository,
                        Role sysAdminRole,
                        PasswordEncoder encoder,
                        Environment environment) {
                boolean enabled = environment.matchesProfiles("local")
                                && environment.getProperty(
                                                "app.e2e-user.enabled",
                                                Boolean.class,
                                                false);
                if (!enabled) {
                        return;
                }

                String username = environment.getRequiredProperty(
                                "app.e2e-user.username");
                String password = environment.getRequiredProperty(
                                "app.e2e-user.password");

                User user = userRepository.findByUsername(username)
                                .orElseGet(() -> User.builder()
                                                .username(username)
                                                .build());
                user.setPassword(encoder.encode(password));
                user.setEnabled(true);
                user.setRoles(Set.of(sysAdminRole));
                userRepository.save(user);

                System.out.println("Local E2E user initialized");
        }
}
