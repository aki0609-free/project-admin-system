package com.project.backend.app.storage.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageBackend {

    private final StorageProperties properties;

    @Override
    public StorageType type() {
        return StorageType.LOCAL;
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolvePath(key));
    }

    @Override
    public InputStream load(String key) {
        try {
            return Files.newInputStream(resolvePath(key));
        } catch (Exception e) {
            throw new RuntimeException("ローカルファイルの読込に失敗しました。 key=" + key, e);
        }
    }

    @Override
    public String save(
            String key,
            InputStream inputStream,
            long size,
            String contentType
    ) {
        try {
            Path path = resolvePath(key);
            Files.createDirectories(path.getParent());
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("ローカルファイルの保存に失敗しました。 key=" + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolvePath(key));
        } catch (Exception e) {
            throw new RuntimeException("ローカルファイルの削除に失敗しました。 key=" + key, e);
        }
    }

    @Override
    public List<String> list(String prefix) {
        try {
            Path dir = resolvePath(prefix);

            if (!Files.exists(dir)) {
                return List.of();
            }

            if (!Files.isDirectory(dir)) {
                throw new RuntimeException("指定されたprefixはディレクトリではありません。 prefix=" + prefix);
            }

            try (Stream<Path> stream = Files.list(dir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> dir.relativize(path).toString())
                        .map(name -> name.replace("\\", "/"))
                        .sorted()
                        .toList();
            }
        } catch (Exception e) {
            throw new RuntimeException("ローカルファイル一覧の取得に失敗しました。 prefix=" + prefix, e);
        }
    }

    private Path resolvePath(String key) {
        Path basePath = Path.of(properties.getLocalBasePath())
                .toAbsolutePath()
                .normalize();

        Path resolvedPath = basePath
                .resolve(key)
                .normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new RuntimeException("不正なファイルキーです。 key=" + key);
        }

        return resolvedPath;
    }
}
