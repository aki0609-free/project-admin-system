package com.project.backend.app.security.auth.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.project.backend.app.security.permission.entity.Permission;
import com.project.backend.app.security.permission.entity.Role;
import com.project.backend.features.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecurityUser implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        if (user.getRoles() == null) {
            return authorities;
        }

        for (Role role : user.getRoles()) {

            authorities.add(
                new SimpleGrantedAuthority("ROLE_" + role.getName())
            );

            if (role.getPermissions() != null) {
                for (Permission permission : role.getPermissions()) {
                    authorities.add(
                        new SimpleGrantedAuthority(permission.getName())
                    );
                }
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    public Long getUserId() {
        return user.getId();
    }

    public User getUser() {
        return this.user;
    }

    public String getTenantId() {
        return user.getTenantId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }
    
}
