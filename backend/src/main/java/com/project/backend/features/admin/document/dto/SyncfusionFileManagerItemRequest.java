package com.project.backend.features.admin.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SyncfusionFileManagerItemRequest(
        String name,
        Boolean isFile,
        String filterPath
) {
}
