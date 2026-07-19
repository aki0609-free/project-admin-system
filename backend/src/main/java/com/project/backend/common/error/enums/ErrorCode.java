package com.project.backend.common.error.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ===== COMMON =====
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "予期しないエラーが発生しました", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMON_RESOURCE_NOT_FOUND_ERROR("COMMON_RESOURCE_NOT_FOUND_ERROR", "リソースが見つかりませんでした", HttpStatus.NOT_FOUND),

    // ===== VALIDATION =====
    VALIDATION_ERROR("VALIDATION_ERROR", "バリデーションエラーです", HttpStatus.BAD_REQUEST),
    VALIDATION_REQUIRED("VALIDATION_REQUIRED", "必須項目です", HttpStatus.BAD_REQUEST),
    VALIDATION_INVALID_FORMAT("VALIDATION_INVALID_FORMAT", "形式が不正です", HttpStatus.BAD_REQUEST),

    // ===== USER =====
    USER_NOT_FOUND("USER_NOT_FOUND", "ユーザーが存在しません", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "ユーザーは既に存在します", HttpStatus.CONFLICT),

    // ===== AUTH =====
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "認証に失敗しました", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "トークンの有効期限が切れています", HttpStatus.UNAUTHORIZED),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getStatus() { return status; }
}
