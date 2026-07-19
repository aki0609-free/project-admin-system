package com.project.backend.app.storage.service;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "project.storage.s3",
        name = "enabled",
        havingValue = "true"
)
public class S3StorageService implements StorageBackend {

    private final S3Client s3Client;
    private final StorageProperties properties;

    @Override
    public StorageType type() {
        return StorageType.S3;
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket())
                            .key(key)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public InputStream load(String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket())
                        .key(key)
                        .build()
        );
    }

    @Override
    public String save(
            String key,
            InputStream inputStream,
            long size,
            String contentType
    ) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(bucket())
                .key(key);

        if (contentType != null && !contentType.isBlank()) {
            builder.contentType(contentType);
        }

        s3Client.putObject(
                builder.build(),
                RequestBody.fromInputStream(inputStream, size)
        );

        return key;
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket())
                        .key(key)
                        .build()
        );
    }

    @SuppressWarnings("null")
    @Override
    public List<String> list(String prefix) {
        String normalizedPrefix = normalizePrefix(prefix);

        ListObjectsV2Request request =
                ListObjectsV2Request.builder()
                        .bucket(bucket())
                        .prefix(normalizedPrefix)
                        .build();

        return s3Client.listObjectsV2(request)
                .contents()
                .stream()
                .map(S3Object::key)
                .filter(key -> !key.endsWith("/"))
                .map(key -> removePrefix(key, normalizedPrefix))
                .filter(name -> !name.isBlank())
                .sorted()
                .toList();
    }

    private String bucket() {
        return properties.getS3().getBucket();
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }

        String value = prefix.trim();

        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        if (!value.endsWith("/")) {
            value += "/";
        }

        return value;
    }

    private String removePrefix(String key, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return key;
        }

        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }

        return key;
    }
}
