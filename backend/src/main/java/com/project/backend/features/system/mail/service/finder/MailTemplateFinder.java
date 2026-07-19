package com.project.backend.features.system.mail.service.finder;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.mail.entity.MailTemplate;
import com.project.backend.features.system.mail.repository.MailTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailTemplateFinder {

    private final MailTemplateRepository repository;

    public MailTemplate getActive(String templateKey) {
        return repository
                .findByTemplateKeyAndActiveFlagTrueAndDeletedAtIsNull(templateKey)
                .orElseThrow(() -> new RuntimeException(
                        "メールテンプレートが見つかりません。 templateKey=" + templateKey
                ));
    }
}