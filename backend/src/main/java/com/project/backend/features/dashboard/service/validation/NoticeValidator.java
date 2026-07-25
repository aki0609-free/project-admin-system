package com.project.backend.features.dashboard.service.validation;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.dashboard.dto.NoticeSaveRequest;

@Service
public class NoticeValidator {

    private static final int TITLE_MAX_LENGTH = 300;
    private static final int CONTENT_MAX_LENGTH = 60_000;
    private static final Set<String> ALLOWED_COLORS = Set.of(
            "blue",
            "red",
            "orange",
            "green",
            "purple"
    );

    public void validateSave(NoticeSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.title())) {
            throw new IllegalArgumentException("タイトルは必須です。");
        }

        if (request.title().trim().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("タイトルは300文字以内で入力してください。");
        }

        if (request.start() == null) {
            throw new IllegalArgumentException("開始日は必須です。");
        }

        if (request.end() == null) {
            throw new IllegalArgumentException("終了日は必須です。");
        }

        if (request.end().isBefore(request.start())) {
            throw new IllegalArgumentException("終了日は開始日以降にしてください。");
        }

        if (StringUtils.hasText(request.color())
                && !ALLOWED_COLORS.contains(request.color())) {
            throw new IllegalArgumentException("指定できない表示色です。");
        }

        if (request.content() != null
                && request.content().length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("内容は60000文字以内で入力してください。");
        }
    }
}
