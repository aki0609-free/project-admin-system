package com.project.backend.features.system.mail.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.common.sanitizer.HtmlSanitizer;
import com.project.backend.features.system.mail.dto.MailTemplatePreviewRequest;
import com.project.backend.features.system.mail.dto.MailTemplatePreviewResponse;
import com.project.backend.features.system.mail.dto.MailTemplateResponse;
import com.project.backend.features.system.mail.dto.MailTemplateSaveRequest;
import com.project.backend.features.system.mail.entity.MailTemplate;
import com.project.backend.features.system.mail.properties.MailProperties;
import com.project.backend.features.system.mail.repository.MailTemplateRepository;
import com.project.backend.features.system.mail.service.validation.MailTemplateAdminValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailTemplateAdminService {

    private final MailTemplateRepository repository;
    private final MailTemplateAdminValidator validator;
    private final MailTemplateRenderService renderService;
    private final HtmlSanitizer htmlSanitizer;
    private final MailProperties properties;

    @Transactional(readOnly = true)
    public List<MailTemplateResponse> findAll() {
        return repository.findAllByDeletedAtIsNullOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MailTemplateResponse findDetail(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public MailTemplateResponse create(MailTemplateSaveRequest request) {
        validator.validateCreate(request, properties);

        MailTemplate entity = new MailTemplate();
        applyRequest(entity, request);

        return toResponse(repository.save(entity));
    }

    @Transactional
    public MailTemplateResponse update(
            Long id,
            MailTemplateSaveRequest request
    ) {
        MailTemplate entity = findEntity(id);
        validator.validateUpdate(entity, request, properties);
        applyRequest(entity, request);

        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        MailTemplate entity = findEntity(id);
        entity.setActiveFlag(false);
        entity.setDeletedAt(Instant.now());
    }

    public MailTemplatePreviewResponse preview(
            MailTemplatePreviewRequest request
    ) {
        validator.validatePreview(request);

        String subject = renderService.render(
                request.subjectTemplate(),
                request.variablesOrEmpty()
        );
        String body = renderService.render(
                request.bodyTemplate(),
                request.variablesOrEmpty()
        );

        if (request.htmlFlag()) {
            body = htmlSanitizer.sanitize(body);
        }

        return new MailTemplatePreviewResponse(
                subject,
                body,
                request.htmlFlag()
        );
    }

    private MailTemplate findEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException(
                        "メールテンプレートが見つかりません。 id=" + id
                ));
    }

    private void applyRequest(
            MailTemplate entity,
            MailTemplateSaveRequest request
    ) {
        entity.setTemplateKey(request.templateKey());
        entity.setTemplateName(request.templateName());
        entity.setFromAddress(properties.getFromAddress());
        entity.setFromName(StringUtils.hasText(request.fromName())
                ? request.fromName()
                : properties.getFromName());
        entity.setSubjectTemplate(request.subjectTemplate());
        entity.setBodyTemplate(request.htmlFlag()
                ? htmlSanitizer.sanitize(request.bodyTemplate())
                : request.bodyTemplate());
        entity.setHtmlFlag(request.htmlFlag());
        entity.setActiveFlag(request.activeFlag());
    }

    private MailTemplateResponse toResponse(MailTemplate entity) {
        return new MailTemplateResponse(
                entity.getId(),
                entity.getTemplateKey(),
                entity.getTemplateName(),
                entity.getFromAddress(),
                entity.getFromName(),
                entity.getSubjectTemplate(),
                entity.getBodyTemplate(),
                entity.isHtmlFlag(),
                entity.isActiveFlag()
        );
    }
}
