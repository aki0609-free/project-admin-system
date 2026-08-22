package com.project.backend.features.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.project.backend.app.security.permission.entity.Role;
import com.project.backend.app.security.permission.repository.RoleRepository;
import com.project.backend.features.user.dto.UserCreateRequest;
import com.project.backend.features.user.dto.UserUpdateRequest;
import com.project.backend.features.user.entity.User;
import com.project.backend.features.user.repository.UserRepository;

class UserCommandServiceTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-08-22T01:02:03Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneId.of("Asia/Tokyo")
    );

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private UserCommandService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserCommandService(
                userRepository,
                roleRepository,
                passwordEncoder,
                FIXED_CLOCK
        );
    }

    @Test
    void create_shouldEncodePasswordAndAssignSelectedRole() {
        Role role = Role.builder().id(1L).name("MANAGER").build();
        when(roleRepository.findByNameIn(Set.of("MANAGER")))
                .thenReturn(Set.of(role));
        when(passwordEncoder.encode("plain-password"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(10L);
                    return user;
                });

        Long id = service.create(createRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(id).isEqualTo(10L);
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRoles()).containsExactly(role);
    }

    @Test
    void update_shouldKeepCurrentPasswordWhenPasswordIsBlank() {
        Role role = Role.builder().id(1L).name("MANAGER").build();
        User user = User.builder()
                .id(10L)
                .username("before")
                .password("encoded-current")
                .enabled(true)
                .roles(Set.of(role))
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findByNameIn(Set.of("MANAGER")))
                .thenReturn(Set.of(role));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setUsername("after");
        request.setPassword(" ");
        request.setEnabled(false);
        request.setRoles(Set.of("MANAGER"));

        service.update(10L, request);

        assertThat(user.getUsername()).isEqualTo("after");
        assertThat(user.getPassword()).isEqualTo("encoded-current");
        assertThat(user.getEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void delete_shouldDisableAndUseApplicationClock() {
        User user = User.builder()
                .id(10L)
                .username("target-user")
                .enabled(true)
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.delete(10L);

        assertThat(user.getEnabled()).isFalse();
        assertThat(user.getDeletedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(user.getUsername())
                .isEqualTo("target-user__deleted__10__20260822100203");
        verify(userRepository).save(user);
    }

    @Test
    void delete_shouldRejectRemovingLastEnabledSystemAdministrator() {
        Role role = Role.builder().id(1L).name("SYS_ADMIN").build();
        User user = User.builder()
                .id(10L)
                .username("only-admin")
                .enabled(true)
                .roles(Set.of(role))
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.existsAnotherEnabledSystemAdministrator(10L))
                .thenReturn(false);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SYS_ADMIN");

        assertThat(user.getEnabled()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
    }

    private UserCreateRequest createRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("new-user");
        request.setPassword("plain-password");
        request.setEnabled(true);
        request.setRoles(Set.of("MANAGER"));
        return request;
    }
}
