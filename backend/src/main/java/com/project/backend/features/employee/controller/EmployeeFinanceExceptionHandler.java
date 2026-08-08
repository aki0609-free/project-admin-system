package com.project.backend.features.employee.controller;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.features.dailyreport.controller.DailyReportController;

@RestControllerAdvice(assignableTypes = {
        EmployeeLoanController.class,
        EmployeeSavingController.class,
        EmployeeFinanceController.class,
        DailyReportController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmployeeFinanceExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "EMPLOYEE_FINANCE_INVALID_REQUEST",
                exception.getMessage(),
                MDC.get("traceId")
        ));
    }
}
