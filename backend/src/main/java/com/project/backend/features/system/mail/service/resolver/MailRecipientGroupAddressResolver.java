package com.project.backend.features.system.mail.service.resolver;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.dto.MailAddressSet;
import com.project.backend.features.system.mail.entity.MailRecipient;
import com.project.backend.features.system.mail.entity.MailRecipientGroup;
import com.project.backend.features.system.mail.enums.MailRecipientType;

@Service
public class MailRecipientGroupAddressResolver {

    public MailAddressSet resolve(
            MailRecipientGroup group,
            String recipientKey,
            List<String> additionalCcAddresses,
            List<String> additionalBccAddresses
    ) {
        List<MailRecipient> recipients = group.getRecipients() == null
                ? List.of()
                : group.getRecipients().stream()
                        .filter(item -> item.getDeletedAt() == null)
                        .filter(MailRecipient::isActiveFlag)
                        .filter(item -> !StringUtils.hasText(recipientKey)
                                || recipientKey.equals(item.getRecipientKey()))
                        .toList();

        return new MailAddressSet(
                extract(recipients, MailRecipientType.TO),
                merge(extract(recipients, MailRecipientType.CC), additionalCcAddresses),
                merge(extract(recipients, MailRecipientType.BCC), additionalBccAddresses)
        );
    }

    private List<String> extract(List<MailRecipient> recipients, MailRecipientType type) {
        return recipients.stream()
                .filter(item -> item.getRecipientType() == type)
                .map(MailRecipient::getEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private List<String> merge(List<String> base, List<String> additional) {
        return Stream.concat(
                        base == null ? Stream.empty() : base.stream(),
                        additional == null ? Stream.empty() : additional.stream()
                )
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}