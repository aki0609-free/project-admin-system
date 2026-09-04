package com.project.backend.features.admin.document.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.admin.document.dto.DocumentDownload;
import com.project.backend.features.admin.document.dto.DocumentEntryResponse;
import com.project.backend.features.admin.document.dto.DocumentListResponse;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerContent;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerDetails;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerDownload;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerPermission;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerRequest;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerResponse;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.enums.DocumentOperation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyncfusionFileManagerService {

    private static final int PAGE_SIZE = 1000;

    private final DocumentManagementService documentService;
    private final DocumentAreaPolicy areaPolicy;
    private final Clock clock;

    public SyncfusionFileManagerResponse execute(
            DocumentArea area,
            SyncfusionFileManagerRequest request
    ) {
        if (request == null || !StringUtils.hasText(request.action())) {
            throw new IllegalArgumentException(
                    "FileManagerのactionは必須です。"
            );
        }

        return switch (request.action().trim().toLowerCase(Locale.ROOT)) {
            case "read" -> read(area, request.path());
            case "create" -> create(area, request);
            case "delete" -> delete(area, request);
            case "rename" -> rename(area, request);
            case "copy" -> copyOrMove(area, request, false);
            case "move" -> copyOrMove(area, request, true);
            case "search" -> search(area, request);
            case "details" -> details(area, request);
            default -> throw new IllegalArgumentException(
                    "未対応のFileManager actionです。 action="
                            + request.action()
            );
        };
    }

    public SyncfusionFileManagerDownload download(
            DocumentArea area,
            SyncfusionFileManagerRequest request
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.DOWNLOAD);

        if (request == null || request.names().isEmpty()) {
            throw new IllegalArgumentException(
                    "ダウンロード対象は必須です。"
            );
        }

        String directoryPath = toRelativePath(request.path());
        List<DownloadTarget> targets = request.names().stream()
                .map(name -> downloadTarget(area, directoryPath, name))
                .toList();

        if (targets.size() == 1 && !targets.getFirst().directory()) {
            DownloadTarget target = targets.getFirst();
            DocumentDownload download = documentService.download(
                    area,
                    target.path()
            );

            return new SyncfusionFileManagerDownload(
                    download.fileName(),
                    download.contentType(),
                    outputStream -> {
                        try (InputStream inputStream =
                                     download.inputStream()) {
                            inputStream.transferTo(outputStream);
                        }
                    }
            );
        }

        String archiveName = targets.size() == 1
                ? targets.getFirst().name() + ".zip"
                : "documents.zip";

        return new SyncfusionFileManagerDownload(
                archiveName,
                "application/zip",
                outputStream -> writeZip(area, targets, outputStream)
        );
    }

    private SyncfusionFileManagerResponse read(
            DocumentArea area,
            String externalPath
    ) {
        String relativePath = toRelativePath(externalPath);
        List<DocumentEntryResponse> entries = listAll(
                area,
                relativePath
        );
        Instant now = Instant.now(clock);
        String currentName = currentDirectoryName(area, relativePath);

        SyncfusionFileManagerContent cwd =
                new SyncfusionFileManagerContent(
                        currentName,
                        0L,
                        now,
                        now,
                        entries.stream().anyMatch(
                                DocumentEntryResponse::directory
                        ),
                        false,
                        "",
                        toExternalDirectoryPath(parentPath(relativePath)),
                        relativePath.isEmpty() ? "root" : relativePath,
                        permission(area)
                );

        return SyncfusionFileManagerResponse.read(
                cwd,
                entries.stream()
                        .map(entry -> toContent(area, entry))
                        .sorted(Comparator
                                .comparing(
                                        SyncfusionFileManagerContent::isFile
                                )
                                .thenComparing(
                                        SyncfusionFileManagerContent::name,
                                        String.CASE_INSENSITIVE_ORDER
                                ))
                        .toList()
        );
    }

    private SyncfusionFileManagerResponse create(
            DocumentArea area,
            SyncfusionFileManagerRequest request
    ) {
        areaPolicy.requireAllowed(
                area,
                DocumentOperation.CREATE_DIRECTORY
        );
        String parent = toRelativePath(request.path());
        String createdPath = join(parent, request.name());
        documentService.createDirectory(area, createdPath);

        return SyncfusionFileManagerResponse.changed(
                List.of(findContent(area, createdPath))
        );
    }

    private SyncfusionFileManagerResponse delete(
            DocumentArea area,
            SyncfusionFileManagerRequest request
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.DELETE);
        String parent = toRelativePath(request.path());
        List<SyncfusionFileManagerContent> deleted = new ArrayList<>();

        for (String name : request.names()) {
            String path = join(parent, name);
            SyncfusionFileManagerContent content = findContent(area, path);
            documentService.delete(area, path, !content.isFile());
            deleted.add(content);
        }

        return SyncfusionFileManagerResponse.changed(deleted);
    }

    private SyncfusionFileManagerResponse rename(
            DocumentArea area,
            SyncfusionFileManagerRequest request
    ) {
        areaPolicy.requireAllowed(area, DocumentOperation.RENAME);
        String parent = toRelativePath(request.path());
        String sourcePath = join(parent, request.name());
        SyncfusionFileManagerContent source = findContent(
                area,
                sourcePath
        );

        documentService.rename(
                area,
                sourcePath,
                request.newName(),
                !source.isFile()
        );

        return SyncfusionFileManagerResponse.changed(
                List.of(findContent(
                        area,
                        join(parent, request.newName())
                ))
        );
    }

    private SyncfusionFileManagerResponse copyOrMove(
            DocumentArea area,
            SyncfusionFileManagerRequest request,
            boolean move
    ) {
        areaPolicy.requireAllowed(
                area,
                move ? DocumentOperation.MOVE : DocumentOperation.COPY
        );
        String sourceParent = toRelativePath(request.path());
        String targetParent = toRelativePath(request.targetPath());
        List<SyncfusionFileManagerContent> changed = new ArrayList<>();

        for (String name : request.names()) {
            String sourcePath = join(sourceParent, name);
            String targetPath = join(targetParent, name);
            SyncfusionFileManagerContent source = findContent(
                    area,
                    sourcePath
            );

            if (move) {
                documentService.move(
                        area,
                        sourcePath,
                        targetPath,
                        !source.isFile()
                );
            } else {
                documentService.copy(
                        area,
                        sourcePath,
                        targetPath,
                        !source.isFile()
                );
            }

            changed.add(findContent(area, targetPath));
        }

        return SyncfusionFileManagerResponse.changed(changed);
    }

    private SyncfusionFileManagerResponse search(
            DocumentArea area,
            SyncfusionFileManagerRequest request
    ) {
        String keyword = Optional.ofNullable(request.searchString())
                .orElse("")
                .replace("*", "")
                .trim();
        String directoryPath = toRelativePath(request.path());
        String prefix = directoryPath.isEmpty()
                ? ""
                : directoryPath + "/";

        List<DocumentEntryResponse> matches = keyword.isEmpty()
                ? documentService.listRecursively(area, directoryPath)
                : documentService.search(area, keyword);

        List<SyncfusionFileManagerContent> files = matches.stream()
                        .filter(entry -> prefix.isEmpty()
                                || entry.path().startsWith(prefix))
                        .map(entry -> toContent(area, entry))
                        .toList();

        return SyncfusionFileManagerResponse.changed(files);
    }

    private SyncfusionFileManagerResponse details(
            DocumentArea area,
            SyncfusionFileManagerRequest request
    ) {
        String parent = toRelativePath(request.path());
        List<SyncfusionFileManagerContent> contents =
                request.names().stream()
                        .map(name -> findContent(area, join(parent, name)))
                        .toList();

        if (contents.isEmpty()) {
            throw new IllegalArgumentException(
                    "詳細表示対象は必須です。"
            );
        }

        boolean multiple = contents.size() > 1;
        long totalSize = contents.stream()
                .mapToLong(SyncfusionFileManagerContent::size)
                .sum();
        SyncfusionFileManagerContent first = contents.getFirst();

        return SyncfusionFileManagerResponse.details(
                new SyncfusionFileManagerDetails(
                        contents.stream()
                                .map(SyncfusionFileManagerContent::name)
                                .collect(java.util.stream.Collectors.joining(
                                        ", "
                                )),
                        toExternalDirectoryPath(parent),
                        formatSize(totalSize),
                        multiple || first.dateCreated() == null
                                ? null
                                : first.dateCreated().toString(),
                        multiple || first.dateModified() == null
                                ? null
                                : first.dateModified().toString(),
                        multiple,
                        multiple ? null : first.isFile(),
                        multiple ? null : first.permission()
                )
        );
    }

    private List<DocumentEntryResponse> listAll(
            DocumentArea area,
            String relativePath
    ) {
        List<DocumentEntryResponse> entries = new ArrayList<>();
        String continuationToken = null;

        do {
            DocumentListResponse page = documentService.list(
                    area,
                    relativePath,
                    continuationToken,
                    PAGE_SIZE
            );
            entries.addAll(page.entries());
            continuationToken = page.nextContinuationToken();
        } while (continuationToken != null);

        return entries;
    }

    private SyncfusionFileManagerContent findContent(
            DocumentArea area,
            String relativePath
    ) {
        String parent = parentPath(relativePath);
        String name = fileName(relativePath);

        return listAll(area, parent).stream()
                .filter(entry -> entry.name().equals(name))
                .findFirst()
                .map(entry -> toContent(area, entry))
                .orElseThrow(() -> new IllegalArgumentException(
                        "操作対象が存在しません。 path=" + relativePath
                ));
    }

    private SyncfusionFileManagerContent toContent(
            DocumentArea area,
            DocumentEntryResponse entry
    ) {
        String parent = parentPath(entry.path());
        Instant modified = entry.lastModified() != null
                ? entry.lastModified()
                : Instant.now(clock);

        return new SyncfusionFileManagerContent(
                entry.name(),
                entry.size(),
                modified,
                modified,
                entry.directory(),
                !entry.directory(),
                entry.directory() ? "" : extension(entry.name()),
                toExternalDirectoryPath(parent),
                entry.path(),
                permission(area)
        );
    }

    private SyncfusionFileManagerPermission permission(
            DocumentArea area
    ) {
        boolean write = areaPolicy.isAllowed(
                area,
                DocumentOperation.DELETE
        );

        return new SyncfusionFileManagerPermission(
                true,
                write,
                areaPolicy.isAllowed(area, DocumentOperation.COPY),
                true,
                areaPolicy.isAllowed(
                        area,
                        DocumentOperation.CREATE_DIRECTORY
                ),
                areaPolicy.isAllowed(area, DocumentOperation.UPLOAD),
                write
                        ? null
                        : "この領域は参照専用です。"
        );
    }

    private DownloadTarget downloadTarget(
            DocumentArea area,
            String parent,
            String name
    ) {
        String path = join(parent, name);
        boolean directory = !findContent(area, path).isFile();

        return new DownloadTarget(
                name,
                path,
                directory
        );
    }

    private void writeZip(
            DocumentArea area,
            List<DownloadTarget> targets,
            OutputStream outputStream
    ) throws java.io.IOException {
        try (ZipOutputStream zipOutputStream =
                     new ZipOutputStream(outputStream)) {
            for (DownloadTarget target : targets) {
                if (target.directory()) {
                    writeDirectoryToZip(
                            area,
                            target,
                            zipOutputStream
                    );
                } else {
                    writeFileToZip(
                            area,
                            target.path(),
                            target.name(),
                            zipOutputStream
                    );
                }
            }
            zipOutputStream.finish();
        }
    }

    private void writeDirectoryToZip(
            DocumentArea area,
            DownloadTarget target,
            ZipOutputStream zipOutputStream
    ) throws java.io.IOException {
        List<DocumentEntryResponse> entries =
                documentService.listRecursively(
                        area,
                        target.path()
                );
        String directoryPrefix = target.name() + "/";
        zipOutputStream.putNextEntry(new ZipEntry(directoryPrefix));
        zipOutputStream.closeEntry();

        for (DocumentEntryResponse entry : entries) {
            String suffix = entry.path().substring(
                    target.path().length()
            );
            while (suffix.startsWith("/")) {
                suffix = suffix.substring(1);
            }
            String zipPath = directoryPrefix + suffix;

            if (entry.directory()) {
                zipOutputStream.putNextEntry(
                        new ZipEntry(ensureTrailingSlash(zipPath))
                );
                zipOutputStream.closeEntry();
            } else {
                writeFileToZip(
                        area,
                        entry.path(),
                        zipPath,
                        zipOutputStream
                );
            }
        }
    }

    private void writeFileToZip(
            DocumentArea area,
            String relativePath,
            String zipPath,
            ZipOutputStream zipOutputStream
    ) throws java.io.IOException {
        DocumentDownload download = documentService.download(
                area,
                relativePath
        );
        zipOutputStream.putNextEntry(new ZipEntry(zipPath));

        try (InputStream inputStream = download.inputStream()) {
            inputStream.transferTo(zipOutputStream);
        }

        zipOutputStream.closeEntry();
    }

    private String currentDirectoryName(
            DocumentArea area,
            String relativePath
    ) {
        return relativePath.isEmpty()
                ? area.getDisplayName()
                : fileName(relativePath);
    }

    private String toRelativePath(String externalPath) {
        if (!StringUtils.hasText(externalPath)
                || "/".equals(externalPath.trim())) {
            return "";
        }

        String value = externalPath.trim().replace("\\", "/");

        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    private String toExternalDirectoryPath(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return "/";
        }
        return "/" + relativePath + "/";
    }

    private String join(String parent, String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("名称は必須です。");
        }
        return StringUtils.hasText(parent)
                ? parent + "/" + name
                : name;
    }

    private String parentPath(String path) {
        int index = path.lastIndexOf("/");
        return index >= 0 ? path.substring(0, index) : "";
    }

    private String fileName(String path) {
        int index = path.lastIndexOf("/");
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private String extension(String name) {
        int index = name.lastIndexOf(".");
        return index >= 0
                ? name.substring(index).toLowerCase(Locale.ROOT)
                : "";
    }

    private String ensureTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(
                    Locale.ROOT,
                    "%.1f MB",
                    bytes / (1024.0 * 1024)
            );
        }
        return String.format(
                Locale.ROOT,
                "%.1f GB",
                bytes / (1024.0 * 1024 * 1024)
        );
    }

    private record DownloadTarget(
            String name,
            String path,
            boolean directory
    ) {
    }
}
