package com.project.backend.features.master.payrollitem.controller;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.features.master.allowance.controller.AllowanceController;
import com.project.backend.features.master.deduction.controller.DeductionController;
import com.project.backend.features.master.payrollitem.exception.PayrollItemMasterConflictException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice(assignableTypes = {
        AllowanceController.class,
        DeductionController.class,
        PayrollRuleOptionController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PayrollItemMasterExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(error("PAYROLL_ITEM_INVALID_REQUEST", exception));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("PAYROLL_ITEM_NOT_FOUND", exception));
    }

    @ExceptionHandler(PayrollItemMasterConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(PayrollItemMasterConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("PAYROLL_ITEM_CONFLICT", exception));
    }

    private ErrorResponse error(String code, RuntimeException exception) {
        return new ErrorResponse(code, exception.getMessage(), MDC.get("traceId"));
    }
}
