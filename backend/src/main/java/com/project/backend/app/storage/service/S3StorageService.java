package com.project.backend.app.storage.service;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.model.StorageEntry;
import com.project.backend.app.storage.model.StorageListPage;
import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
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
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw storageException("S3ファイルの存在確認に失敗しました。 key=" + key, e);
        }
    }

    @Override
    public boolean directoryExists(String key) {
        String normalizedPrefix = normalizePrefix(key);

        if (normalizedPrefix.isBlank()) {
            return false;
        }

        if (exists(normalizedPrefix)) {
            return true;
        }

        ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(bucket())
                        .prefix(normalizedPrefix)
                        .maxKeys(1)
                        .build()
        );

        return !response.contents().isEmpty();
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

        return s3Client.listObjectsV2Paginator(request)
                .contents()
                .stream()
                .map(S3Object::key)
                .filter(key -> !key.endsWith("/"))
                .map(key -> removePrefix(key, normalizedPrefix))
                .filter(name -> !name.isBlank())
                .sorted()
                .toList();
    }

    @Override
    public StorageListPage listDirectory(
            String prefix,
            String continuationToken,
            int maxKeys
    ) {
        validateMaxKeys(maxKeys);

        String normalizedPrefix = normalizePrefix(prefix);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket())
                .prefix(normalizedPrefix)
                .delimiter("/")
                .continuationToken(continuationToken)
                .maxKeys(maxKeys)
                .build();

        var response = s3Client.listObjectsV2(request);
        List<StorageEntry> entries = new ArrayList<>();

        response.commonPrefixes().forEach(commonPrefix -> {
            String key = removeTrailingSlash(commonPrefix.prefix());
            entries.add(new StorageEntry(
                    key,
                    fileName(key),
                    true,
                    0,
                    null,
                    null
            ));
        });

        response.contents().stream()
                .filter(object -> !object.key().equals(normalizedPrefix))
                .filter(object -> !object.key().endsWith("/"))
                .map(this::toStorageEntry)
                .forEach(entries::add);

        entries.sort(Comparator
                .comparing(StorageEntry::directory)
                .reversed()
                .thenComparing(StorageEntry::name));

        return new StorageListPage(
                entries,
                response.nextContinuationToken(),
                Boolean.TRUE.equals(response.isTruncated())
        );
    }

    @Override
    public List<StorageEntry> listRecursively(String prefix) {
        String normalizedPrefix = normalizePrefix(prefix);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket())
                .prefix(normalizedPrefix)
                .build();

        return s3Client.listObjectsV2Paginator(request)
                .contents()
                .stream()
                .filter(object -> !object.key().equals(normalizedPrefix))
                .map(this::toStorageEntry)
                .sorted(Comparator.comparing(StorageEntry::key))
                .toList();
    }

    @Override
    public void createDirectory(String key) {
        String directoryKey = normalizePrefix(key);

        if (directoryKey.isBlank()) {
            throw new IllegalArgumentException(
                    "S3ディレクトリキーは必須です。"
            );
        }

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket())
                        .key(directoryKey)
                        .contentType("application/x-directory")
                        .build(),
                RequestBody.empty()
        );
    }

    @Override
    public void copy(
            String sourceKey,
            String targetKey
    ) {
        s3Client.copyObject(
                CopyObjectRequest.builder()
                        .bucket(bucket())
                        .copySource(encodeCopySource(sourceKey))
                        .key(targetKey)
                        .build()
        );
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

    private StorageEntry toStorageEntry(S3Object object) {
        String key = object.key();
        boolean directory = key.endsWith("/");
        String normalizedKey = directory ? removeTrailingSlash(key) : key;

        return new StorageEntry(
                normalizedKey,
                fileName(normalizedKey),
                directory,
                directory ? 0 : object.size(),
                object.lastModified(),
                object.eTag()
        );
    }

    private String fileName(String key) {
        int index = key.lastIndexOf("/");
        return index >= 0 ? key.substring(index + 1) : key;
    }

    private String removeTrailingSlash(String value) {
        String result = value;

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private String encodeCopySource(String sourceKey) {
        return URLEncoder.encode(
                bucket() + "/" + sourceKey,
                StandardCharsets.UTF_8
        )
                .replace("+", "%20")
                .replace("%2F", "/");
    }

    private void validateMaxKeys(int maxKeys) {
        if (maxKeys < 1 || maxKeys > 1000) {
            throw new IllegalArgumentException(
                    "maxKeys は1から1000の範囲で指定してください。"
            );
        }
    }

    private RuntimeException storageException(
            String message,
            Exception cause
    ) {
        return new RuntimeException(message, cause);
    }
}
