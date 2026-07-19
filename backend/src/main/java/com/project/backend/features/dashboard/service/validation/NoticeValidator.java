package com.project.backend.features.dashboard.service.validation;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.dashboard.dto.NoticeSaveRequest;

@Service
public class NoticeValidator {

    public void validateSave(NoticeSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.title())) {
            throw new IllegalArgumentException("タイトルは必須です。");
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
    }
}