package com.project.backend.features.system.mail.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.dto.MailTestSendRequest;
import com.project.backend.features.system.mail.properties.MailProperties;

@Component
public class MailTestSendValidator {

    public void validate(MailTestSendRequest request, MailProperties properties) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.toAddressesOrEmpty().stream().noneMatch(StringUtils::hasText)) {
            throw new RuntimeException("toAddresses は必須です。");
        }

        if (!StringUtils.hasText(request.subject())) {
            throw new RuntimeException("subject は必須です。");
        }

        if (!StringUtils.hasText(request.body())) {
            throw new RuntimeException("body は必須です。");
        }

        if (!StringUtils.hasText(properties.getFromAddress())) {
            throw new RuntimeException("project.mail.from-address が設定されていません。");
        }
    }
}