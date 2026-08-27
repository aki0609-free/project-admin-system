package com.project.backend.features.system.mail.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.mail.dto.MailPdfSendRequest;
import com.project.backend.features.system.mail.enums.MailStorageType;
import com.project.backend.features.system.mail.properties.MailProperties;

class MailPdfSendValidatorTest {

    @Test
    void validate_shouldCheckTheRequestedStorageBackend() {
        StorageService storageService = mock(StorageService.class);
        MailPdfSendValidator validator = new MailPdfSendValidator(storageService);
        MailProperties properties = properties();
        MailPdfSendRequest request = request(MailStorageType.S3);
        when(storageService.exists(StorageType.S3, "reports/sample.pdf"))
                .thenReturn(true);

        validator.validate(request, properties);

        verify(storageService).exists(StorageType.S3, "reports/sample.pdf");
    }

    @Test
    void validate_shouldRejectMissingStorageType() {
        MailPdfSendValidator validator = new MailPdfSendValidator(mock(StorageService.class));

        assertThatThrownBy(() -> validator.validate(request(null), properties()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("storageType は必須です。");
    }

    private MailPdfSendRequest request(MailStorageType storageType) {
        return new MailPdfSendRequest(
                "PDF_MAIL",
                null,
                null,
                null,
                List.of("recipient@example.com"),
                null,
                List.of(),
                List.of(),
                "subject",
                "body",
                false,
                storageType,
                "reports/sample.pdf",
                "sample.pdf"
        );
    }

    private MailProperties properties() {
        MailProperties properties = new MailProperties();
        properties.setFromAddress("sender@example.com");
        return properties;
    }
}
