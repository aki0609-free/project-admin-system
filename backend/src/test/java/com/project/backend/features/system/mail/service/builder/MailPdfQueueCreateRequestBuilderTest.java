package com.project.backend.features.system.mail.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.mail.dto.MailPdfSendRequest;
import com.project.backend.features.system.mail.enums.MailStorageType;
import com.project.backend.features.system.mail.properties.MailProperties;

class MailPdfQueueCreateRequestBuilderTest {

    private final MailPdfQueueCreateRequestBuilder builder =
            new MailPdfQueueCreateRequestBuilder();

    @Test
    void build_shouldKeepCcAndBccAddressesAndSelectedBodyFormat() {
        MailProperties properties = new MailProperties();
        properties.setFromAddress("sender@example.com");
        properties.setFromName("送信者");

        var result = builder.build(
                new MailPdfSendRequest(
                        "PDF_MAIL",
                        "PDF:1",
                        "CUSTOMER_INVOICE_10",
                        null,
                        List.of("to@example.com"),
                        null,
                        List.of("cc@example.com"),
                        List.of("bcc@example.com"),
                        "件名",
                        "<p>本文</p>",
                        true,
                        MailStorageType.S3,
                        "documents/report.pdf",
                        "report.pdf"
                ),
                properties
        );

        assertThat(result.toAddresses()).containsExactly("to@example.com");
        assertThat(result.ccAddresses()).containsExactly("cc@example.com");
        assertThat(result.bccAddresses()).containsExactly("bcc@example.com");
        assertThat(result.htmlFlag()).isTrue();
    }
}
