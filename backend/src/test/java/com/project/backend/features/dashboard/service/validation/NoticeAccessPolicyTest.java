package com.project.backend.features.dashboard.service.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.exception.NoticeConflictException;

class NoticeAccessPolicyTest {

    private final NoticeAccessPolicy policy = new NoticeAccessPolicy();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireEditable_shouldRejectAutoNotice() {
        Notice notice = notice(NoticeSourceType.AUTO);

        assertThatThrownBy(() -> policy.requireEditable(notice))
                .isInstanceOf(NoticeConflictException.class)
                .hasMessageContaining("編集できません");
    }

    @Test
    void requireDeletable_shouldRejectAutoNoticeForAdmin() {
        authenticate("ROLE_ADMIN");

        assertThatThrownBy(() ->
                policy.requireDeletable(notice(NoticeSourceType.AUTO)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireDeletable_shouldAllowAutoNoticeForSysAdmin() {
        authenticate("ROLE_SYS_ADMIN");

        assertThatCode(() ->
                policy.requireDeletable(notice(NoticeSourceType.AUTO)))
                .doesNotThrowAnyException();
    }

    @Test
    void requireDeletable_shouldAllowManualNoticeForAdmin() {
        authenticate("ROLE_ADMIN");

        assertThatCode(() ->
                policy.requireDeletable(notice(NoticeSourceType.MANUAL)))
                .doesNotThrowAnyException();
    }

    private Notice notice(NoticeSourceType sourceType) {
        Notice notice = new Notice();
        notice.setSourceType(sourceType);
        return notice;
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        "password",
                        List.of(new SimpleGrantedAuthority(authority))
                )
        );
    }
}
