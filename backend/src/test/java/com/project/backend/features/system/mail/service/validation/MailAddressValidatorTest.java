package com.project.backend.features.system.mail.service.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MailAddressValidatorTest {

    @Test
    void validate_shouldAcceptNormalAddress() {
        assertThatCode(() -> MailAddressValidator.validate(
                "email",
                "user@example.com"
        )).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectInvalidAddress() {
        assertThatThrownBy(() -> MailAddressValidator.validate(
                "email",
                "invalid-address"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("形式が不正");
    }
}
