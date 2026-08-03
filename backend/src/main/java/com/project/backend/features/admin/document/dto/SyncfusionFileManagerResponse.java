package com.project.backend.features.admin.document.dto;

import java.util.List;

public record SyncfusionFileManagerResponse(
        SyncfusionFileManagerContent cwd,
        List<SyncfusionFileManagerContent> files,
        SyncfusionFileManagerError error,
        SyncfusionFileManagerDetails details
) {

    public SyncfusionFileManagerResponse {
        files = files == null ? null : List.copyOf(files);
    }

    public static SyncfusionFileManagerResponse read(
            SyncfusionFileManagerContent cwd,
            List<SyncfusionFileManagerContent> files
    ) {
        return new SyncfusionFileManagerResponse(
                cwd,
                files,
                null,
                null
        );
    }

    public static SyncfusionFileManagerResponse changed(
            List<SyncfusionFileManagerContent> files
    ) {
        return new SyncfusionFileManagerResponse(
                null,
                files,
                null,
                null
        );
    }

    public static SyncfusionFileManagerResponse details(
            SyncfusionFileManagerDetails details
    ) {
        return new SyncfusionFileManagerResponse(
                null,
                null,
                null,
                details
        );
    }
}
