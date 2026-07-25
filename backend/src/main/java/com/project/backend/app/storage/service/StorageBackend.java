package com.project.backend.app.storage.service;

import java.io.InputStream;
import java.util.List;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.model.StorageEntry;
import com.project.backend.app.storage.model.StorageListPage;

/**
 * StorageTypeごとの物理ストレージ操作を定義する。
 */
public interface StorageBackend {

    StorageType type();

    boolean exists(String key);

    boolean directoryExists(String key);

    InputStream load(String key);

    String save(
            String key,
            InputStream inputStream,
            long size,
            String contentType
    );

    void delete(String key);

    List<String> list(String prefix);

    StorageListPage listDirectory(
            String prefix,
            String continuationToken,
            int maxKeys
    );

    List<StorageEntry> listRecursively(String prefix);

    void createDirectory(String key);

    void copy(
            String sourceKey,
            String targetKey
    );
}
