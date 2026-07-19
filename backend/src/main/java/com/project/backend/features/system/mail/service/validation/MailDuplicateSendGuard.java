package com.project.backend.features.system.mail.service.validation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailSendStatus;
import com.project.backend.features.system.mail.repository.MailSendQueueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailDuplicateSendGuard {

    private static final String DUPLICATE_MESSAGE =
            "同一businessKeyのSENT済みメールが存在するため、重複送信を防止しました。";

    private final MailSendQueueRepository repository;

    public void validateBeforeCreate(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            return;
        }

        if (repository.existsByBusinessKeyAndStatusAndDeletedAtIsNull(
                businessKey,
                MailSendStatus.SENT
        )) {
            throw new RuntimeException(message(businessKey));
        }
    }

    public boolean hasSentDuplicateForUpdate(MailSendQueue current) {
        if (current == null || !StringUtils.hasText(current.getBusinessKey())) {
            return false;
        }

        List<MailSendQueue> sameBusinessKeyQueues =
                repository.findAllByBusinessKeyAndDeletedAtIsNullOrderByIdAsc(
                        current.getBusinessKey()
                );

        return sameBusinessKeyQueues.stream()
                .anyMatch(queue -> queue.getStatus() == MailSendStatus.SENT
                        && !sameId(queue.getId(), current.getId()));
    }

    public String message(String businessKey) {
        return DUPLICATE_MESSAGE + " businessKey=" + businessKey;
    }

    private boolean sameId(Long left, Long right) {
        return left != null && left.equals(right);
    }
}
