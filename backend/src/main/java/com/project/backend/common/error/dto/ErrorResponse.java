package com.project.backend.common.error.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class ErrorResponse {
    private String code;
    private String message;
    private String traceId;
    private List<String> details;

    public ErrorResponse(String code, String message, String traceId) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
    }

    public ErrorResponse(String code, String message, String traceId, List<String> details) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.details = details;
    }
}