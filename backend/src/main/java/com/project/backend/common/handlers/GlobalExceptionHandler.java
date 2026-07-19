package com.project.backend.common.handlers;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.common.error.enums.ErrorCode;
import com.project.backend.common.exception.base.BusinessException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import static net.logstash.logback.argument.StructuredArguments.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @SuppressWarnings("null")
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {

        ErrorCode code = ErrorCode.COMMON_INTERNAL_ERROR;
        String traceId = MDC.get("traceId");

        log.error(
                "request failed",
                keyValue("errorCode", code.getCode()),
                keyValue("path", request.getRequestURI()),
                keyValue("traceId", traceId),
                keyValue("exception", ex.getClass().getSimpleName()),
                keyValue("message", ex.getMessage()),
                ex
        );

        return ResponseEntity
                .status(code.getStatus())
                .body(new ErrorResponse(
                        code.getCode(),
                        code.getMessage(),
                        traceId));
    }

    @SuppressWarnings("null")
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        ErrorCode code = ErrorCode.COMMON_RESOURCE_NOT_FOUND_ERROR;
        String traceId = MDC.get("traceId");

        log.warn(
                "resource not found",
                keyValue("errorCode", code.getCode()),
                keyValue("path", request.getRequestURI()),
                keyValue("traceId", traceId),
                keyValue("message", ex.getMessage())
        );

        return ResponseEntity
                .status(code.getStatus())
                .body(new ErrorResponse(
                        code.getCode(),
                        code.getMessage(),
                        traceId));
    }

    @SuppressWarnings("null")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        String traceId = MDC.get("traceId");

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        log.warn(
                "validation error",
                keyValue("errorCode", code.getCode()),
                keyValue("path", request.getRequestURI()),
                keyValue("traceId", traceId),
                keyValue("errors", errors)
        );

        return ResponseEntity
                .status(code.getStatus())
                .body(new ErrorResponse(
                        code.getCode(),
                        code.getMessage(),
                        traceId,
                        errors));
    }

    @SuppressWarnings("null")
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request) {

        ErrorCode code = ex.getErrorCode();
        String traceId = MDC.get("traceId");

        log.warn(
                "business error",
                keyValue("errorCode", code.getCode()),
                keyValue("path", request.getRequestURI()),
                keyValue("traceId", traceId),
                keyValue("message", ex.getMessage())
        );

        return ResponseEntity
                .status(code.getStatus())
                .body(new ErrorResponse(
                        code.getCode(),
                        ex.getMessage(),
                        traceId));
    }
}