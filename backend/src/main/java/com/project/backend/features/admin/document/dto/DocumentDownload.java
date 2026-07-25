package com.project.backend.features.admin.document.dto;

import java.io.InputStream;

public record DocumentDownload(
        String fileName,
        String contentType,
        InputStream inputStream
) {
}
