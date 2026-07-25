package com.project.backend.features.dashboard.service.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dashboard.dto.NoticeSaveRequest;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeType;

class NoticeValidatorTest {

    private final NoticeValidator validator = new NoticeValidator();

    @Test
    void validateSave_shouldAcceptSupportedValues() {
        assertThatCode(() ->
                validator.validateSave(request(
                        "お知らせ",
                        "blue",
                        "本文"
                )))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSave_shouldRejectUnsupportedColor() {
        assertThatThrownBy(() ->
                validator.validateSave(request(
                        "お知らせ",
                        "javascript:red",
                        "本文"
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("表示色");
    }

    @Test
    void validateSave_shouldRejectOverlongTitleAndContent() {
        assertThatThrownBy(() ->
                validator.validateSave(request(
                        "a".repeat(301),
                        "blue",
                        "本文"
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300文字");

        assertThatThrownBy(() ->
                validator.validateSave(request(
                        "お知らせ",
                        "blue",
                        "a".repeat(60_001)
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("60000文字");
    }

    private NoticeSaveRequest request(
            String title,
            String color,
            String content
    ) {
        LocalDate today = LocalDate.of(2026, 7, 25);
        return new NoticeSaveRequest(
                title,
                today,
                today,
                NoticeType.INFO,
                color,
                NoticeContentFormat.PLAIN_TEXT,
                content,
                false,
                true
        );
    }
}
