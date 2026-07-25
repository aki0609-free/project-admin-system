package com.project.backend.features.admin.document.service;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.app.storage.model.StorageEntry;
import com.project.backend.app.storage.model.StorageListPage;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.dto.DocumentAreaResponse;
import com.project.backend.features.admin.document.dto.DocumentDownload;
import com.project.backend.features.admin.document.dto.DocumentEntryResponse;
import com.project.backend.features.admin.document.dto.DocumentListResponse;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.enums.DocumentOperation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentManagementService {

    private final StorageService storageService;
    private final DocumentAreaPolicy areaPolicy;
    private final DocumentStorageKeyResolver keyResolver;

    public List<DocumentAreaResponse> findAreas() {
        return areaPolicy.findAreas();
    }

    public DocumentListResponse list(
            DocumentArea area,
            String relativePath,
            String continuationToken,
            int maxKeys
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.READ);
        String prefix = keyResolver.resolve(area, relativePath);
        StorageListPage page = storageService.listDirectory(
                prefix,
                continuationToken,
                maxKeys
        );

        return new DocumentListResponse(
                page.entries().stream()
                        .map(entry -> toResponse(area, entry))
                        .toList(),
                page.nextContinuationToken(),
                page.truncated()
        );
    }

    public List<DocumentEntryResponse> search(
            DocumentArea area,
            String keyword
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.SEARCH);

        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("検索キーワードは必須です。");
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        String root = keyResolver.resolveAreaRoot(area);

        return storageService.listRecursively(root).stream()
                .filter(entry -> entry.name()
                        .toLowerCase(Locale.ROOT)
                        .contains(normalizedKeyword))
                .map(entry -> toResponse(area, entry))
                .sorted(Comparator.comparing(DocumentEntryResponse::path))
                .toList();
    }

    public DocumentDownload download(
            DocumentArea area,
            String relativePath
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.DOWNLOAD);
        requireNonRootPath(relativePath);

        String key = keyResolver.resolve(area, relativePath);

        if (!storageService.exists(key)) {
            throw new IllegalArgumentException(
                    "ダウンロード対象のファイルが存在しません。 path="
                            + relativePath
            );
        }

        String fileName = fileName(relativePath);
        String contentType = URLConnection.guessContentTypeFromName(fileName);

        return new DocumentDownload(
                fileName,
                contentType != null
                        ? contentType
                        : "application/octet-stream",
                storageService.load(key)
        );
    }

    public void createDirectory(
            DocumentArea area,
            String relativePath
    ) {
        areaPolicy.requireAllowed(
                area,
                DocumentOperation.CREATE_DIRECTORY
        );
        requireNonRootPath(relativePath);

        String key = keyResolver.resolve(area, relativePath);
        requireTargetDoesNotExist(key);
        storageService.createDirectory(key);
    }

    public void upload(
            DocumentArea area,
            String directoryPath,
            MultipartFile file
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.UPLOAD);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "アップロードファイルは必須です。"
            );
        }

        String originalFileName = validateName(
                file.getOriginalFilename(),
                "ファイル名"
        );
        String relativePath = joinRelativePath(
                directoryPath,
                originalFileName
        );
        String key = keyResolver.resolve(area, relativePath);

        try (InputStream inputStream = file.getInputStream()) {
            storageService.save(
                    key,
                    inputStream,
                    file.getSize(),
                    file.getContentType()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "書類のアップロードに失敗しました。 path="
                            + relativePath,
                    e
            );
        }
    }

    public void copy(
            DocumentArea area,
            String sourcePath,
            String targetPath,
            boolean directory
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.COPY);
        copyInternal(area, sourcePath, targetPath, directory);
    }

    public void move(
            DocumentArea area,
            String sourcePath,
            String targetPath,
            boolean directory
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.MOVE);
        copyInternal(area, sourcePath, targetPath, directory);
        deleteInternal(area, sourcePath, directory);
    }

    public void rename(
            DocumentArea area,
            String sourcePath,
            String newName,
            boolean directory
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.RENAME);
        requireNonRootPath(sourcePath);

        String validatedName = validateName(newName, "変更後名称");
        String parent = parentPath(sourcePath);
        String targetPath = joinRelativePath(parent, validatedName);

        copyInternal(area, sourcePath, targetPath, directory);
        deleteInternal(area, sourcePath, directory);
    }

    public void delete(
            DocumentArea area,
            String relativePath,
            boolean directory
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.DELETE);
        deleteInternal(area, relativePath, directory);
    }

    private void copyInternal(
            DocumentArea area,
            String sourcePath,
            String targetPath,
            boolean directory
    ) {
        requireNonRootPath(sourcePath);
        requireNonRootPath(targetPath);

        String sourceKey = keyResolver.resolve(area, sourcePath);
        String targetKey = keyResolver.resolve(area, targetPath);

        if (sourceKey.equals(targetKey)) {
            throw new IllegalArgumentException(
                    "コピー元とコピー先が同じです。"
            );
        }

        if (directory && targetKey.startsWith(sourceKey + "/")) {
            throw new IllegalArgumentException(
                    "フォルダ自身の配下へコピーまたは移動できません。"
            );
        }

        requireSourceExists(sourceKey, directory);
        requireTargetDoesNotExist(targetKey);

        if (!directory) {
            storageService.copy(sourceKey, targetKey);
            return;
        }

        storageService.createDirectory(targetKey);

        for (StorageEntry entry : storageService.listRecursively(sourceKey)) {
            String suffix = entry.key().substring(sourceKey.length());
            String destinationKey = targetKey + suffix;

            if (entry.directory()) {
                storageService.createDirectory(destinationKey);
            } else {
                storageService.copy(entry.key(), destinationKey);
            }
        }
    }

    private void deleteInternal(
            DocumentArea area,
            String relativePath,
            boolean directory
    ) {
        requireNonRootPath(relativePath);
        String key = keyResolver.resolve(area, relativePath);
        requireSourceExists(key, directory);

        if (!directory) {
            storageService.delete(key);
            return;
        }

        storageService.listRecursively(key).stream()
                .sorted(Comparator
                        .comparingInt((StorageEntry entry) ->
                                entry.key().length())
                        .reversed())
                .forEach(entry ->
                        storageService.delete(
                                entry.directory()
                                        ? entry.key() + "/"
                                        : entry.key()
                        ));

        storageService.delete(key + "/");
        storageService.delete(key);
    }

    private void requireSourceExists(
            String key,
            boolean directory
    ) {
        boolean exists = directory
                ? storageService.directoryExists(key)
                : storageService.exists(key);

        if (!exists) {
            throw new IllegalArgumentException(
                    "操作対象が存在しません。 key=" + key
            );
        }
    }

    private void requireTargetDoesNotExist(String key) {
        if (storageService.exists(key)
                || storageService.directoryExists(key)) {
            throw new IllegalArgumentException(
                    "操作先に同名の書類またはフォルダが存在します。 key="
                            + key
            );
        }
    }

    private DocumentEntryResponse toResponse(
            DocumentArea area,
            StorageEntry entry
    ) {
        String root = keyResolver.resolveAreaRoot(area);
        String path = entry.key();

        if (path.equals(root)) {
            path = "";
        } else if (path.startsWith(root + "/")) {
            path = path.substring(root.length() + 1);
        } else {
            throw new IllegalStateException(
                    "書類領域外のストレージキーが返却されました。 key="
                            + entry.key()
            );
        }

        return new DocumentEntryResponse(
                path,
                entry.name(),
                entry.directory(),
                entry.size(),
                entry.lastModified(),
                entry.eTag()
        );
    }

    private void requireNonRootPath(String relativePath) {
        if (!StringUtils.hasText(relativePath)
                || "/".equals(relativePath.trim())) {
            throw new IllegalArgumentException(
                    "書類領域のルートは操作対象にできません。"
            );
        }
    }

    private String validateName(
            String name,
            String label
    ) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException(label + "は必須です。");
        }

        String value = name.trim();

        if (".".equals(value)
                || "..".equals(value)
                || value.contains("/")
                || value.contains("\\")
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + "が不正です。");
        }

        return value;
    }

    private String joinRelativePath(
            String directoryPath,
            String name
    ) {
        if (!StringUtils.hasText(directoryPath)
                || "/".equals(directoryPath.trim())) {
            return name;
        }

        String directory = directoryPath.trim()
                .replace("\\", "/");

        while (directory.endsWith("/")) {
            directory = directory.substring(
                    0,
                    directory.length() - 1
            );
        }

        return directory + "/" + name;
    }

    private String parentPath(String path) {
        String normalized = path.trim().replace("\\", "/");
        int index = normalized.lastIndexOf("/");
        return index >= 0 ? normalized.substring(0, index) : "";
    }

    private String fileName(String path) {
        String normalized = path.trim().replace("\\", "/");
        int index = normalized.lastIndexOf("/");
        return index >= 0
                ? normalized.substring(index + 1)
                : normalized;
    }
}
