package com.project.backend.app.storage.service;

import java.io.InputStream;
import java.util.List;

import com.project.backend.app.storage.enums.StorageType;

/**
 * StorageTypeごとの物理ストレージ操作を定義する。
 */
public interface StorageBackend {

    StorageType type();

    boolean exists(String key);

    InputStream load(String key);

    String save(
            String key,
            InputStream inputStream,
            long size,
            String contentType
    );

    void delete(String key);

    List<String> list(String prefix);
}
