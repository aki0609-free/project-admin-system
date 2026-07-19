package com.project.backend.features.system.mail.dto;

import java.util.List;

public record MailAddressSet(
        List<String> toAddresses,
        List<String> ccAddresses,
        List<String> bccAddresses
) {
}