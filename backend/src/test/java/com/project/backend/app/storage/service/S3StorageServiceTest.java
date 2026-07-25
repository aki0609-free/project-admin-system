package com.project.backend.app.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.app.storage.model.StorageListPage;
import com.project.backend.app.storage.properties.StorageProperties;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

class S3StorageServiceTest {

    private S3Client s3Client;
    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);

        StorageProperties properties = new StorageProperties();
        properties.getS3().setBucket("project-admin-test-documents");
        storageService = new S3StorageService(s3Client, properties);
    }

    @Test
    void exists_shouldReturnFalseOnlyForNotFound() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(404)
                        .message("not found")
                        .build());

        assertThat(storageService.exists("documents/general/missing.pdf"))
                .isFalse();
    }

    @Test
    void exists_shouldNotHideAccessDeniedAsMissing() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(403)
                        .message("access denied")
                        .build());

        assertThatThrownBy(() ->
                storageService.exists("documents/general/private.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("存在確認に失敗");
    }

    @Test
    void listDirectory_shouldReturnVirtualFolderAndFile() {
        Instant lastModified = Instant.parse("2026-07-25T00:00:00Z");

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder()
                        .commonPrefixes(CommonPrefix.builder()
                                .prefix(
                                        "documents/general/contracts/"
                                )
                                .build())
                        .contents(S3Object.builder()
                                .key("documents/general/readme.txt")
                                .size(12L)
                                .lastModified(lastModified)
                                .eTag("etag")
                                .build())
                        .isTruncated(false)
                        .build());

        StorageListPage result = storageService.listDirectory(
                "documents/general",
                null,
                100
        );

        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries().get(0).directory()).isTrue();
        assertThat(result.entries().get(0).name()).isEqualTo("contracts");
        assertThat(result.entries().get(1).directory()).isFalse();
        assertThat(result.entries().get(1).name()).isEqualTo("readme.txt");
    }

    @Test
    void directoryExists_shouldRecognizeVirtualS3Directory() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(404)
                        .message("marker not found")
                        .build());
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder()
                        .contents(S3Object.builder()
                                .key(
                                        "documents/general/contracts/"
                                                + "sample.pdf"
                                )
                                .size(10L)
                                .build())
                        .build());

        assertThat(storageService.directoryExists(
                "documents/general/contracts"
        )).isTrue();
    }

    @Test
    void copy_shouldUrlEncodeSourceKey() {
        storageService.copy(
                "documents/general/契約 書.pdf",
                "documents/general/copied.pdf"
        );

        ArgumentCaptor<CopyObjectRequest> captor =
                ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(captor.capture());

        assertThat(captor.getValue().copySource())
                .isEqualTo(
                        "project-admin-test-documents/documents/general/"
                                + "%E5%A5%91%E7%B4%84%20%E6%9B%B8.pdf"
                );
        assertThat(captor.getValue().key())
                .isEqualTo("documents/general/copied.pdf");
    }
}
