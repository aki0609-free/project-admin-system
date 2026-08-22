package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeLoanSaveRequest {

    @NotNull(message = "従業員は必須です。")
    private Long employeeId;

    @NotNull(message = "借入元本は必須です。")
    @DecimalMin(value = "0.01", message = "借入元本は0円より大きい金額を指定してください。")
    private BigDecimal principal = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "月返済額は0円以上で指定してください。")
    private BigDecimal monthlyRepayment = BigDecimal.ZERO;

    private LocalDate loanDate;

    private LocalDate repaymentStartDate;

    private boolean activeFlag = true;

}
