package com.project.backend.features.dashboard.service.validation;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.exception.NoticeConflictException;

@Component
public class NoticeAccessPolicy {

    private static final String SYS_ADMIN_AUTHORITY = "ROLE_SYS_ADMIN";

    public void requireEditable(Notice notice) {
        if (notice.getSourceType() == NoticeSourceType.AUTO) {
            throw new NoticeConflictException(
                    "自動生成されたお知らせは編集できません。お知らせRuleを変更してください。"
            );
        }
    }

    public void requireDeletable(Notice notice) {
        if (notice.getSourceType() != NoticeSourceType.AUTO) {
            return;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean sysAdmin = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                SYS_ADMIN_AUTHORITY.equals(authority.getAuthority())
                        );

        if (!sysAdmin) {
            throw new AccessDeniedException(
                    "自動生成されたお知らせを削除できるのはSYS_ADMINだけです。"
            );
        }
    }
}
