package com.project.backend.features.system.mail.service.validation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.dto.MailRecipientGroupSaveRequest;
import com.project.backend.features.system.mail.dto.MailRecipientSaveRequest;
import com.project.backend.features.system.mail.repository.MailRecipientGroupRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailRecipientGroupValidator {

    private final MailRecipientGroupRepository repository;

    public void validateSaveRequest(MailRecipientGroupSaveRequest request, Long id) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.groupKey())) {
            throw new RuntimeException("groupKey は必須です。");
        }

        if (!StringUtils.hasText(request.groupName())) {
            throw new RuntimeException("groupName は必須です。");
        }

        boolean exists = id == null
                ? repository.existsByGroupKeyAndDeletedAtIsNull(request.groupKey())
                : repository.existsByGroupKeyAndIdNotAndDeletedAtIsNull(request.groupKey(), id);

        if (exists) {
            throw new RuntimeException("groupKey が重複しています。 groupKey=" + request.groupKey());
        }

        for (MailRecipientSaveRequest recipient : safeRecipients(request)) {
            if (!StringUtils.hasText(recipient.email())) {
                throw new RuntimeException("email は必須です。");
            }
        }
    }

    private List<MailRecipientSaveRequest> safeRecipients(MailRecipientGroupSaveRequest request) {
        return request.recipients() != null ? request.recipients() : List.of();
    }
}