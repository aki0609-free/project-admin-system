package com.project.backend.features.dashboard.controller;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.features.dashboard.exception.NoticeConflictException;

@RestControllerAdvice(assignableTypes = NoticeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NoticeExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "NOTICE_INVALID_REQUEST",
                exception.getMessage(),
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(NoticeConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            NoticeConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "NOTICE_CONFLICT",
                        exception.getMessage(),
                        MDC.get("traceId")
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "NOTICE_ACCESS_DENIED",
                        exception.getMessage(),
                        MDC.get("traceId")
                ));
    }
}
