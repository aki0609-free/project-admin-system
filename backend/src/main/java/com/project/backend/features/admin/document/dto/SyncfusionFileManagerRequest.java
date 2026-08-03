package com.project.backend.features.admin.document.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SyncfusionFileManagerRequest(
        String action,
        String path,
        String name,
        String newName,
        List<String> names,
        String targetPath,
        String searchString,
        List<String> renameFiles,
        List<SyncfusionFileManagerItemRequest> data
) {

    public SyncfusionFileManagerRequest {
        names = names == null ? List.of() : List.copyOf(names);
        renameFiles = renameFiles == null
                ? List.of()
                : List.copyOf(renameFiles);
        data = data == null ? List.of() : List.copyOf(data);
    }
}
