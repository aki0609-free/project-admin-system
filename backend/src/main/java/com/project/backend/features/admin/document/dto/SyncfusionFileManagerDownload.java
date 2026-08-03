package com.project.backend.features.admin.document.dto;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record SyncfusionFileManagerDownload(
        String fileName,
        String contentType,
        StreamingResponseBody body
) {
}
