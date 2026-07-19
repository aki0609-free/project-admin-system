package com.project.backend.features.system.mail.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.mail.dto.MailTemplateSaveRequest;
import com.project.backend.features.system.mail.entity.MailTemplate;
import com.project.backend.features.system.mail.properties.MailProperties;
import com.project.backend.features.system.mail.repository.MailTemplateRepository;

class MailTemplateAdminValidatorTest {

    @Test
    void validateUpdate_shouldRejectTemplateKeyChange() {
        MailTemplateAdminValidator validator = new MailTemplateAdminValidator(
                mock(MailTemplateRepository.class)
        );
        MailTemplate entity = new MailTemplate();
        entity.setTemplateKey("ORIGINAL_KEY");
        MailTemplateSaveRequest request = new MailTemplateSaveRequest(
                "CHANGED_KEY",
                "テンプレート",
                "ProjectAdmin",
                "件名",
                "本文",
                false,
                true
        );

        assertThatThrownBy(() -> validator.validateUpdate(
                entity,
                request,
                properties()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("変更できません");
    }

    @Test
    void validateCreate_shouldRejectLowercaseTemplateKey() {
        MailTemplateAdminValidator validator = new MailTemplateAdminValidator(
                mock(MailTemplateRepository.class)
        );
        MailTemplateSaveRequest request = new MailTemplateSaveRequest(
                "invalid-key",
                "テンプレート",
                "ProjectAdmin",
                "件名",
                "本文",
                false,
                true
        );

        assertThatThrownBy(() -> validator.validateCreate(request, properties()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("半角英大文字");
    }

    private MailProperties properties() {
        MailProperties properties = new MailProperties();
        properties.setFromAddress("configured@example.com");
        return properties;
    }
}
