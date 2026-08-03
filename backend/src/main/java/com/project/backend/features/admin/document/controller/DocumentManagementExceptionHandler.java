package com.project.backend.features.admin.document.controller;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.features.admin.document.exception.DocumentOperationNotAllowedException;

@RestControllerAdvice(
        assignableTypes = {
                DocumentManagementController.class,
                SyncfusionFileManagerController.class
        }
)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentManagementExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "DOCUMENT_INVALID_REQUEST",
                exception.getMessage(),
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(DocumentOperationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotAllowed(
            DocumentOperationNotAllowedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "DOCUMENT_OPERATION_NOT_ALLOWED",
                        exception.getMessage(),
                        MDC.get("traceId")
                ));
    }
}
