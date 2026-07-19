package com.project.backend.features.system.mail.service.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.mail.dto.MailAttachmentResource;
import com.project.backend.features.system.mail.entity.MailSendAttachment;
import com.project.backend.features.system.mail.enums.MailStorageType;

class StorageMailAttachmentLoaderTest {

    @Test
    void load_shouldReadFromS3_whenAttachmentStorageTypeIsS3() throws Exception {
        StorageService storageService = mock(StorageService.class);
        StorageMailAttachmentLoader loader = new StorageMailAttachmentLoader(storageService);
        MailSendAttachment attachment = attachment(MailStorageType.S3);
        when(storageService.load(StorageType.S3, "reports/file.pdf"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        MailAttachmentResource resource = loader.load(attachment);

        assertThat(resource.fileName()).isEqualTo("file.pdf");
        assertThat(resource.mimeType()).isEqualTo("application/pdf");
        assertThat(resource.source().getInputStream().readAllBytes())
                .containsExactly(1, 2, 3);
        verify(storageService).load(StorageType.S3, "reports/file.pdf");
    }

    @Test
    void exists_shouldCheckLocal_whenAttachmentStorageTypeIsLocal() {
        StorageService storageService = mock(StorageService.class);
        StorageMailAttachmentLoader loader = new StorageMailAttachmentLoader(storageService);
        MailSendAttachment attachment = attachment(MailStorageType.LOCAL);
        when(storageService.exists(StorageType.LOCAL, "reports/file.pdf"))
                .thenReturn(true);

        assertThat(loader.exists(attachment)).isTrue();
        verify(storageService).exists(StorageType.LOCAL, "reports/file.pdf");
    }

    @Test
    void load_shouldRejectMissingStorageType() {
        StorageMailAttachmentLoader loader = new StorageMailAttachmentLoader(
                mock(StorageService.class)
        );
        MailSendAttachment attachment = attachment(null);

        assertThatThrownBy(() -> loader.load(attachment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storageType");
    }

    private MailSendAttachment attachment(MailStorageType storageType) {
        MailSendAttachment attachment = new MailSendAttachment();
        attachment.setStorageType(storageType);
        attachment.setFileKey("reports/file.pdf");
        attachment.setFileName("file.pdf");
        attachment.setMimeType("application/pdf");
        return attachment;
    }
}
