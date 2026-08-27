package com.project.backend.features.system.imports.service.resolver;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.imports.properties.ImportScriptProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportScriptPathResolver {

    private final StorageProperties storageProperties;
    private final StorageService storageService;
    private final ImportScriptProperties scriptProperties;

    public ResolvedImportScript resolve(String scriptPath) {
        if (!StringUtils.hasText(scriptPath)) {
            throw new RuntimeException("scriptPath が設定されていません。");
        }

        String relativePath = normalizeRelativePath(scriptPath);

        return storageService.type() == StorageType.LOCAL
                ? resolveLocal(relativePath)
                : materializeFromStorage(relativePath);
    }

    private ResolvedImportScript resolveLocal(String relativePath) {

        Path basePath = Path.of(storageProperties.getLocalBasePath())
                .toAbsolutePath()
                .normalize();

        Path scriptBasePath = basePath
                .resolve(storageProperties.getImports().getScript().getPath())
                .normalize();

        Path resolvedPath = scriptBasePath
                .resolve(relativePath)
                .normalize();

        if (!resolvedPath.startsWith(scriptBasePath)) {
            throw new RuntimeException("不正なscriptPathです。 path=" + relativePath);
        }

        if (!Files.exists(resolvedPath)) {
            throw new RuntimeException("scriptPath が存在しません。 path=" + resolvedPath);
        }

        if (!Files.isRegularFile(resolvedPath)) {
            throw new RuntimeException("scriptPath がファイルではありません。 path=" + resolvedPath);
        }

        validateFileSize(resolvedPath);
        return ResolvedImportScript.local(resolvedPath);
    }

    private ResolvedImportScript materializeFromStorage(
            String relativePath
    ) {
        String storageKey = scriptStorageKey(relativePath);
        StorageType storageType = storageService.type();

        if (!storageService.exists(storageType, storageKey)) {
            throw new RuntimeException(
                    "S3上に取込スクリプトが存在しません。 key="
                            + storageKey
            );
        }

        Path workRoot = Path.of(scriptProperties.getWorkDirectory())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(workRoot);
            Path tempDirectory = Files.createTempDirectory(
                    workRoot,
                    "script-"
            );
            Path targetPath = tempDirectory
                    .resolve(Path.of(relativePath).getFileName().toString())
                    .normalize();

            if (!targetPath.startsWith(tempDirectory)) {
                throw new RuntimeException("スクリプトの一時保存先が不正です。");
            }

            try (InputStream inputStream = storageService.load(
                    storageType,
                    storageKey
            ); OutputStream outputStream = Files.newOutputStream(
                    targetPath,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            )) {
                copyWithSizeLimit(inputStream, outputStream);
            } catch (Exception e) {
                ResolvedImportScript.temporary(
                        targetPath,
                        tempDirectory
                ).close();
                throw e;
            }

            return ResolvedImportScript.temporary(
                    targetPath,
                    tempDirectory
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "取込スクリプトの一時展開に失敗しました。 key="
                            + storageKey,
                    e
            );
        }
    }

    private String normalizeRelativePath(String scriptPath) {
        String value = scriptPath.trim().replace("\\", "/");
        String prefix = normalizedScriptPrefix();

        if (value.startsWith(prefix + "/")) {
            value = value.substring(prefix.length() + 1);
        }

        if (!StringUtils.hasText(value) || value.startsWith("/")) {
            throw new RuntimeException("不正なscriptPathです。 path=" + scriptPath);
        }

        Path normalized = Path.of(value).normalize();
        if (normalized.isAbsolute()
                || normalized.startsWith("..")
                || normalized.toString().contains("\u0000")) {
            throw new RuntimeException("不正なscriptPathです。 path=" + scriptPath);
        }

        String normalizedValue = normalized.toString().replace("\\", "/");
        String lowerCase = normalizedValue.toLowerCase(Locale.ROOT);
        if (!lowerCase.endsWith(".py") && !lowerCase.endsWith(".sh")) {
            throw new RuntimeException(
                    "実行できるスクリプトは.pyまたは.shだけです。 path="
                            + scriptPath
            );
        }

        return normalizedValue;
    }

    private String scriptStorageKey(String relativePath) {
        return normalizedScriptPrefix() + "/" + relativePath;
    }

    private String normalizedScriptPrefix() {
        String prefix = storageProperties.getImports()
                .getScript()
                .getPath()
                .trim()
                .replace("\\", "/");

        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private void validateFileSize(Path path) {
        try {
            long size = Files.size(path);
            if (size > scriptProperties.getMaxScriptBytes()) {
                throw new RuntimeException(
                        "取込スクリプトのサイズ上限を超えています。 size="
                                + size
                );
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("スクリプトサイズを確認できません。", e);
        }
    }

    private void copyWithSizeLimit(
            InputStream inputStream,
            OutputStream outputStream
    ) throws java.io.IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;

        while ((read = inputStream.read(buffer)) >= 0) {
            total += read;
            if (total > scriptProperties.getMaxScriptBytes()) {
                throw new IllegalArgumentException(
                        "取込スクリプトのサイズ上限を超えています。"
                );
            }
            outputStream.write(buffer, 0, read);
        }

    }
}
