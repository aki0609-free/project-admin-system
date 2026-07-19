package com.project.backend.features.system.mail.service.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.dto.MailTemplatePreviewRequest;
import com.project.backend.features.system.mail.dto.MailTemplateSaveRequest;
import com.project.backend.features.system.mail.entity.MailTemplate;
import com.project.backend.features.system.mail.properties.MailProperties;
import com.project.backend.features.system.mail.repository.MailTemplateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailTemplateAdminValidator {

    private static final Pattern TEMPLATE_KEY_PATTERN =
            Pattern.compile("^[A-Z0-9_]+$");

    private final MailTemplateRepository repository;

    public void validateCreate(
            MailTemplateSaveRequest request,
            MailProperties properties
    ) {
        validateCommon(request, properties);

        if (repository.existsByTemplateKey(request.templateKey())) {
            throw new RuntimeException(
                    "templateKey が重複しています。 templateKey="
                            + request.templateKey()
            );
        }
    }

    public void validateUpdate(
            MailTemplate entity,
            MailTemplateSaveRequest request,
            MailProperties properties
    ) {
        validateCommon(request, properties);

        if (!entity.getTemplateKey().equals(request.templateKey())) {
            throw new RuntimeException("templateKey は作成後に変更できません。");
        }
    }

    public void validatePreview(MailTemplatePreviewRequest request) {
        if (request == null) {
            throw new RuntimeException("プレビューリクエストが不正です。");
        }

        if (!StringUtils.hasText(request.subjectTemplate())) {
            throw new RuntimeException("subjectTemplate は必須です。");
        }

        if (!StringUtils.hasText(request.bodyTemplate())) {
            throw new RuntimeException("bodyTemplate は必須です。");
        }
    }

    private void validateCommon(
            MailTemplateSaveRequest request,
            MailProperties properties
    ) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.templateKey())) {
            throw new RuntimeException("templateKey は必須です。");
        }

        if (!TEMPLATE_KEY_PATTERN.matcher(request.templateKey()).matches()) {
            throw new RuntimeException(
                    "templateKey は半角英大文字・数字・アンダースコアで入力してください。"
            );
        }

        if (!StringUtils.hasText(request.templateName())) {
            throw new RuntimeException("templateName は必須です。");
        }

        if (!StringUtils.hasText(request.subjectTemplate())) {
            throw new RuntimeException("subjectTemplate は必須です。");
        }

        if (!StringUtils.hasText(request.bodyTemplate())) {
            throw new RuntimeException("bodyTemplate は必須です。");
        }

        if (!StringUtils.hasText(properties.getFromAddress())) {
            throw new RuntimeException("project.mail.from-address が設定されていません。");
        }
    }
}
