package com.project.backend.features.system.mail.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.mapper.MailQueueMapper;
import com.project.backend.features.system.mail.repository.MailSendQueueRepository;
import com.project.backend.features.system.mail.service.builder.MailQueueCreateRequestBuilder;
import com.project.backend.features.system.mail.service.finder.MailRecipientGroupFinder;
import com.project.backend.features.system.mail.service.finder.MailTemplateFinder;
import com.project.backend.features.system.mail.service.resolver.MailRecipientGroupAddressResolver;
import com.project.backend.features.system.mail.service.validation.MailDuplicateSendGuard;
import com.project.backend.features.system.mail.service.validation.MailQueueCreateValidator;

class MailQueueCreateServiceTest {

    @Test
    void createDraft_shouldStopBeforeMapping_whenBusinessKeyWasAlreadySent() {
        MailQueueMapper mapper = mock(MailQueueMapper.class);
        MailDuplicateSendGuard duplicateGuard = mock(MailDuplicateSendGuard.class);
        MailQueueCreateRequest request = MailQueueCreateRequest.builder()
                .businessKey("PAYSLIP:2026-07:1")
                .build();
        RuntimeException duplicate = new RuntimeException("duplicate");
        org.mockito.Mockito.doThrow(duplicate)
                .when(duplicateGuard)
                .validateBeforeCreate(request.businessKey());
        MailQueueCreateService service = new MailQueueCreateService(
                mock(MailSendQueueRepository.class),
                mock(MailTemplateRenderService.class),
                mock(MailRecipientGroupAddressResolver.class),
                mapper,
                mock(MailQueueCreateValidator.class),
                mock(MailQueueCreateRequestBuilder.class),
                mock(MailTemplateFinder.class),
                mock(MailRecipientGroupFinder.class),
                duplicateGuard
        );

        assertThatThrownBy(() -> service.createDraft(request))
                .isSameAs(duplicate);

        verify(mapper, never()).toEntity(request);
    }
}
