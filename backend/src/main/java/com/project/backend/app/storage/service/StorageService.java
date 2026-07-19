package com.project.backend.app.storage.service;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.properties.StorageProperties;

@Service
public class StorageService {

    private final Map<StorageType, StorageBackend> backends;
    private final StorageProperties properties;

    public StorageService(
            List<StorageBackend> storageBackends,
            StorageProperties properties
    ) {
        this.properties = properties;
        this.backends = new EnumMap<>(StorageType.class);

        for (StorageBackend storageBackend : storageBackends) {
            StorageBackend previous = backends.put(
                    storageBackend.type(),
                    storageBackend
            );

            if (previous != null) {
                throw new IllegalStateException(
                        "同じStorageTypeのStorageBackendが複数登録されています。 storageType="
                                + storageBackend.type()
                );
            }
        }
    }

    public StorageType type() {
        return properties.resolveDefaultType();
    }

    public boolean exists(String key) {
        return exists(type(), key);
    }

    public InputStream load(String key) {
        return load(type(), key);
    }

    public String save(
            String key,
            InputStream inputStream,
            long size,
            String contentType
    ) {
        return save(type(), key, inputStream, size, contentType);
    }

    public void delete(String key) {
        delete(type(), key);
    }

    public List<String> list(String prefix) {
        return list(type(), prefix);
    }

    public boolean exists(
            StorageType storageType,
            String key
    ) {
        return backend(storageType).exists(key);
    }

    public InputStream load(
            StorageType storageType,
            String key
    ) {
        return backend(storageType).load(key);
    }

    public String save(
            StorageType storageType,
            String key,
            InputStream inputStream,
            long size,
            String contentType
    ) {
        return backend(storageType).save(
                key,
                inputStream,
                size,
                contentType
        );
    }

    public void delete(
            StorageType storageType,
            String key
    ) {
        backend(storageType).delete(key);
    }

    public List<String> list(
            StorageType storageType,
            String prefix
    ) {
        return backend(storageType).list(prefix);
    }

    private StorageBackend backend(StorageType storageType) {
        if (storageType == null) {
            throw new IllegalArgumentException("storageType が未設定です。");
        }

        StorageBackend storageBackend = backends.get(storageType);

        if (storageBackend == null) {
            throw new IllegalStateException(
                    "指定されたStorageTypeのStorageBackendが登録されていません。 storageType="
                            + storageType
            );
        }

        return storageBackend;
    }
}
