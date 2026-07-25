package com.project.backend.features.system.notice.controller;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.features.system.notice.exception.NoticeRuleConflictException;

@RestControllerAdvice(assignableTypes = {
        NoticeRuleController.class,
        NoticeRuleGenerateController.class,
        NoticeRuleScheduleController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NoticeRuleExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "NOTICE_RULE_INVALID_REQUEST",
                exception.getMessage(),
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(NoticeRuleConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            NoticeRuleConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "NOTICE_RULE_CONFLICT",
                        exception.getMessage(),
                        MDC.get("traceId")
                ));
    }
}
