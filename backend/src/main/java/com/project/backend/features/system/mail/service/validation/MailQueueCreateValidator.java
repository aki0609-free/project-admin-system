package com.project.backend.features.system.mail.service.validation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.dto.MailTemplateQueueCreateRequest;

@Component
public class MailQueueCreateValidator {

    public void validateCreateRequest(MailQueueCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("メールキュー作成リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.fromAddress())) {
            throw new RuntimeException("fromAddress は必須です。");
        }

        if (!hasAddresses(request.toAddresses())) {
            throw new RuntimeException("toAddresses は必須です。");
        }

        if (!StringUtils.hasText(request.subject())) {
            throw new RuntimeException("subject は必須です。");
        }

        if (!StringUtils.hasText(request.body())) {
            throw new RuntimeException("body は必須です。");
        }
    }

    public void validateTemplateRequest(MailTemplateQueueCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("メールテンプレートキュー作成リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.templateKey())) {
            throw new RuntimeException("templateKey は必須です。");
        }
    }

    public boolean hasAddresses(List<String> addresses) {
        return addresses != null && addresses.stream().anyMatch(StringUtils::hasText);
    }
}