package com.project.backend.common.util;

import java.util.Base64;

import org.springframework.util.StringUtils;

public final class ApplicationBase64Utils {

    private ApplicationBase64Utils() {
    }

    public static byte[] decodeDataUrlOrBase64(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value;

        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1);
        }

        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Base64形式が不正です。", e);
        }
    }

    public static String encodeDataUrl(byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            return null;
        }

        String resolvedContentType =
                StringUtils.hasText(contentType) ? contentType : "application/octet-stream";

        return "data:"
                + resolvedContentType
                + ";base64,"
                + Base64.getEncoder().encodeToString(data);
    }
}