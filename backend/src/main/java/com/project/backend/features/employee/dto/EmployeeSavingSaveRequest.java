package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.ApprovalStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeSavingSaveRequest {

    @NotNull(message = "従業員は必須です。")
    private Long employeeId;

    @DecimalMin(value = "0.00", message = "積立率は0%以上で指定してください。")
    @DecimalMax(value = "100.00", message = "積立率は100%以下で指定してください。")
    private BigDecimal percentage = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "最低給与額は0円以上で指定してください。")
    private BigDecimal minSalaryThreshold = BigDecimal.ZERO;

    private BigDecimal currentBalance = BigDecimal.ZERO;

    private boolean activeFlag = true;

    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private String approvalComment;
}
