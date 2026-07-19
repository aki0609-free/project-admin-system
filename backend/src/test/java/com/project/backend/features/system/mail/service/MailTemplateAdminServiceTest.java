package com.project.backend.features.system.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.common.sanitizer.HtmlSanitizer;
import com.project.backend.features.system.mail.dto.MailTemplatePreviewRequest;
import com.project.backend.features.system.mail.dto.MailTemplatePreviewResponse;
import com.project.backend.features.system.mail.dto.MailTemplateResponse;
import com.project.backend.features.system.mail.dto.MailTemplateSaveRequest;
import com.project.backend.features.system.mail.entity.MailTemplate;
import com.project.backend.features.system.mail.properties.MailProperties;
import com.project.backend.features.system.mail.repository.MailTemplateRepository;
import com.project.backend.features.system.mail.service.validation.MailTemplateAdminValidator;

class MailTemplateAdminServiceTest {

    @Test
    void create_shouldUseConfiguredSenderAndSanitizeHtml() {
        MailTemplateRepository repository = mock(MailTemplateRepository.class);
        MailTemplateAdminValidator validator = mock(MailTemplateAdminValidator.class);
        MailProperties properties = properties();
        MailTemplateAdminService service = service(repository, validator, properties);
        when(repository.save(any(MailTemplate.class))).thenAnswer(invocation -> {
            MailTemplate entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });
        MailTemplateSaveRequest request = new MailTemplateSaveRequest(
                "PAY_SLIP_NOTICE",
                "給与明細通知",
                "給与担当",
                "{{recipientName}}様",
                "<p>本文</p><script>alert('xss')</script>",
                true,
                true
        );

        MailTemplateResponse response = service.create(request);

        assertThat(response.fromAddress()).isEqualTo("configured@example.com");
        assertThat(response.fromName()).isEqualTo("給与担当");
        assertThat(response.bodyTemplate()).contains("<p>本文</p>");
        assertThat(response.bodyTemplate()).doesNotContain("<script>");
        verify(validator).validateCreate(request, properties);
    }

    @Test
    void preview_shouldRenderVariablesAndSanitizeHtml() {
        MailTemplateAdminValidator validator = mock(MailTemplateAdminValidator.class);
        MailTemplateAdminService service = service(
                mock(MailTemplateRepository.class),
                validator,
                properties()
        );
        MailTemplatePreviewRequest request = new MailTemplatePreviewRequest(
                "{{recipientName}}様",
                "<p>{{fileName}}</p><script>alert('xss')</script>",
                true,
                Map.of(
                        "recipientName", "山田太郎",
                        "fileName", "給与明細.pdf"
                )
        );

        MailTemplatePreviewResponse response = service.preview(request);

        assertThat(response.subject()).isEqualTo("山田太郎様");
        assertThat(response.body()).contains("<p>給与明細.pdf</p>");
        assertThat(response.body()).doesNotContain("<script>");
        verify(validator).validatePreview(request);
    }

    private MailTemplateAdminService service(
            MailTemplateRepository repository,
            MailTemplateAdminValidator validator,
            MailProperties properties
    ) {
        return new MailTemplateAdminService(
                repository,
                validator,
                new MailTemplateRenderService(),
                new HtmlSanitizer(),
                properties
        );
    }

    private MailProperties properties() {
        MailProperties properties = new MailProperties();
        properties.setFromAddress("configured@example.com");
        properties.setFromName("ProjectAdmin");
        return properties;
    }
}
