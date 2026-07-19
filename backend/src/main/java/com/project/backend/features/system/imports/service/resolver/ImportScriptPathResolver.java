package com.project.backend.features.system.imports.service.resolver;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportScriptPathResolver {

    private final StorageProperties storageProperties;

    public Path resolve(String scriptPath) {
        if (!StringUtils.hasText(scriptPath)) {
            throw new RuntimeException("scriptPath が設定されていません。");
        }

        Path basePath = Path.of(storageProperties.getLocalBasePath())
                .toAbsolutePath()
                .normalize();

        Path scriptBasePath = basePath
                .resolve(storageProperties.getImports().getScript().getPath())
                .normalize();

        Path resolvedPath = scriptBasePath
                .resolve(scriptPath)
                .normalize();

        if (!resolvedPath.startsWith(scriptBasePath)) {
            throw new RuntimeException("不正なscriptPathです。 path=" + scriptPath);
        }

        if (!Files.exists(resolvedPath)) {
            throw new RuntimeException("scriptPath が存在しません。 path=" + resolvedPath);
        }

        if (!Files.isRegularFile(resolvedPath)) {
            throw new RuntimeException("scriptPath がファイルではありません。 path=" + resolvedPath);
        }

        return resolvedPath;
    }
}