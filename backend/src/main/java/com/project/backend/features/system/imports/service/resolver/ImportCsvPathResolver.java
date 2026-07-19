package com.project.backend.features.system.imports.service.resolver;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportCsvPathResolver {

    private final StorageProperties storageProperties;

    public Path resolveExisting(String fixedFilePath) {
        Path path = resolve(fixedFilePath);

        if (!Files.exists(path)) {
            throw new RuntimeException("CSVファイルが存在しません。 path=" + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new RuntimeException("CSV取込対象がファイルではありません。 path=" + path);
        }

        return path;
    }

    public Path resolve(String fixedFilePath) {
        if (!StringUtils.hasText(fixedFilePath)) {
            throw new RuntimeException("fixedFilePath が設定されていません。");
        }

        Path basePath = Path.of(storageProperties.getLocalBasePath())
                .toAbsolutePath()
                .normalize();

        Path csvBasePath = basePath
                .resolve(storageProperties.getImports().getCsv().getPath())
                .normalize();

        Path resolvedPath = csvBasePath
                .resolve(fixedFilePath)
                .normalize();

        if (!resolvedPath.startsWith(csvBasePath)) {
            throw new RuntimeException("不正なfixedFilePathです。 path=" + fixedFilePath);
        }

        return resolvedPath;
    }
}