package com.project.backend.app.security.jwt.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.user.entity.User;
import com.project.backend.features.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public SecurityUser loadUserByUsernameAndTenantId(
        String username,
        String tenantId
    ) throws UsernameNotFoundException {

        User user = userRepository
                .findByUsernameAndTenantId(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return new SecurityUser(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        String tenantId = TenantContext.getTenantId();

        User user = userRepository
                    .findByUsernameAndTenantId(username, tenantId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new SecurityUser(user);
    }
}
