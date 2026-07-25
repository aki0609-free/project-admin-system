package com.project.backend.features.system.rule.controller;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.features.system.rule.exception.RuleConflictException;

@RestControllerAdvice(assignableTypes = {
        RuleMasterController.class,
        RuleExecutionController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RuleExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "RULE_INVALID_REQUEST",
                exception.getMessage(),
                MDC.get("traceId")
        ));
    }

    @ExceptionHandler(RuleConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            RuleConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "RULE_CONFLICT",
                        exception.getMessage(),
                        MDC.get("traceId")
                ));
    }
}
