package com.project.backend.features.system.mail.service.finder;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.entity.MailRecipientGroup;
import com.project.backend.features.system.mail.repository.MailRecipientGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailRecipientGroupFinder {

    private final MailRecipientGroupRepository repository;

    public MailRecipientGroup getById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException(
                        "メール宛先グループが見つかりません。 id=" + id
                ));
    }

    public MailRecipientGroup getActive(String groupKey) {
        if (!StringUtils.hasText(groupKey)) {
            throw new RuntimeException("toAddresses または recipientGroupKey は必須です。");
        }

        return repository
                .findByGroupKeyAndActiveFlagTrueAndDeletedAtIsNull(groupKey)
                .orElseThrow(() -> new RuntimeException(
                        "宛先グループが見つかりません。 groupKey=" + groupKey
                ));
    }
}