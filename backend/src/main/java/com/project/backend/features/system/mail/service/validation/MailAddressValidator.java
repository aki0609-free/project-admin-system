package com.project.backend.features.system.mail.service.validation;

import java.util.List;

import jakarta.mail.internet.InternetAddress;

public final class MailAddressValidator {

    private MailAddressValidator() {
    }

    public static void validateAll(String fieldName, List<String> addresses) {
        if (addresses == null) {
            return;
        }

        addresses.stream()
                .filter(address -> address != null && !address.isBlank())
                .forEach(address -> validate(fieldName, address));
    }

    public static void validate(String fieldName, String address) {
        try {
            InternetAddress parsed = new InternetAddress(address.trim(), true);
            parsed.validate();
        } catch (Exception e) {
            throw new RuntimeException(
                    fieldName + "のメールアドレス形式が不正です。 value=" + address,
                    e
            );
        }
    }
}
