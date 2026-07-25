package com.project.backend.app.storage.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.model.StorageEntry;
import com.project.backend.app.storage.model.StorageListPage;
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
    public boolean directoryExists(String key) {
        return Files.isDirectory(resolvePath(key));
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

    @Override
    public StorageListPage listDirectory(
            String prefix,
            String continuationToken,
            int maxKeys
    ) {
        validateMaxKeys(maxKeys);

        Path directory = resolvePath(prefix);

        if (!Files.exists(directory)) {
            return new StorageListPage(List.of(), null, false);
        }

        if (!Files.isDirectory(directory)) {
            throw new RuntimeException(
                    "指定されたprefixはディレクトリではありません。 prefix="
                            + prefix
            );
        }

        int offset = parseContinuationToken(continuationToken);

        try (Stream<Path> stream = Files.list(directory)) {
            List<StorageEntry> allEntries = stream
                    .map(this::toStorageEntry)
                    .sorted(Comparator
                            .comparing(StorageEntry::directory)
                            .reversed()
                            .thenComparing(StorageEntry::name))
                    .toList();

            if (offset > allEntries.size()) {
                throw new IllegalArgumentException(
                        "continuationToken が不正です。"
                );
            }

            int end = Math.min(offset + maxKeys, allEntries.size());
            List<StorageEntry> entries = allEntries.subList(offset, end);
            boolean truncated = end < allEntries.size();

            return new StorageListPage(
                    entries,
                    truncated ? String.valueOf(end) : null,
                    truncated
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "ローカルディレクトリ一覧の取得に失敗しました。 prefix="
                            + prefix,
                    e
            );
        }
    }

    @Override
    public List<StorageEntry> listRecursively(String prefix) {
        Path directory = resolvePath(prefix);

        if (!Files.exists(directory)) {
            return List.of();
        }

        if (!Files.isDirectory(directory)) {
            return List.of(toStorageEntry(directory));
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            return stream
                    .filter(path -> !path.equals(directory))
                    .map(this::toStorageEntry)
                    .sorted(Comparator.comparing(StorageEntry::key))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(
                    "ローカルファイルの再帰一覧取得に失敗しました。 prefix="
                            + prefix,
                    e
            );
        }
    }

    @Override
    public void createDirectory(String key) {
        try {
            Files.createDirectories(resolvePath(key));
        } catch (Exception e) {
            throw new RuntimeException(
                    "ローカルディレクトリの作成に失敗しました。 key="
                            + key,
                    e
            );
        }
    }

    @Override
    public void copy(
            String sourceKey,
            String targetKey
    ) {
        Path source = resolvePath(sourceKey);
        Path target = resolvePath(targetKey);

        if (!Files.isRegularFile(source)) {
            throw new RuntimeException(
                    "コピー元ファイルが存在しません。 key="
                            + sourceKey
            );
        }

        try {
            Files.createDirectories(target.getParent());
            Files.copy(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "ローカルファイルのコピーに失敗しました。 sourceKey="
                            + sourceKey
                            + ", targetKey="
                            + targetKey,
                    e
            );
        }
    }

    private StorageEntry toStorageEntry(Path path) {
        try {
            Path basePath = basePath();
            boolean directory = Files.isDirectory(path);
            String key = basePath
                    .relativize(path.toAbsolutePath().normalize())
                    .toString()
                    .replace("\\", "/");

            return new StorageEntry(
                    key,
                    path.getFileName().toString(),
                    directory,
                    directory ? 0 : Files.size(path),
                    Files.getLastModifiedTime(path).toInstant(),
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "ローカルファイル情報の取得に失敗しました。 path="
                            + path,
                    e
            );
        }
    }

    private int parseContinuationToken(String continuationToken) {
        if (continuationToken == null || continuationToken.isBlank()) {
            return 0;
        }

        try {
            int value = Integer.parseInt(continuationToken);

            if (value < 0) {
                throw new IllegalArgumentException(
                        "continuationToken が不正です。"
                );
            }

            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "continuationToken が不正です。",
                    e
            );
        }
    }

    private void validateMaxKeys(int maxKeys) {
        if (maxKeys < 1 || maxKeys > 1000) {
            throw new IllegalArgumentException(
                    "maxKeys は1から1000の範囲で指定してください。"
            );
        }
    }

    private Path resolvePath(String key) {
        Path basePath = basePath();

        Path resolvedPath = basePath
                .resolve(key)
                .normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new RuntimeException("不正なファイルキーです。 key=" + key);
        }

        return resolvedPath;
    }

    private Path basePath() {
        return Path.of(properties.getLocalBasePath())
                .toAbsolutePath()
                .normalize();
    }
}
