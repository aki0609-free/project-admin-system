package com.project.backend.features.employee.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.EmploymentType;
import com.project.backend.features.employee.enums.Gender;
import com.project.backend.features.employee.enums.DormitoryType;

import lombok.Builder;

@Builder
public record EmployeeDetailResponse(
        Long id,
        String employeeCode,
        String employeeName,
        String employeeNameKana,
        Gender gender,
        LocalDate birthDate,
        LocalDate hireDate,
        LocalDate resignDate,
        EmploymentType employmentType,
        EmploymentStatus employmentStatus,
        String phone,
        String email,
        String postalCode,
        String address,
        boolean dormitoryFlag,
        DormitoryType dormitoryType,
        BigDecimal dormitoryOpeningDays,
        BigDecimal dormitoryCurrentMonthDays,
        BigDecimal dormitoryConsumedDays,
        BigDecimal dormitoryRemainingDays,
        boolean activeFlag,
        EmployeePayrollProfileResponse payrollProfile,
        EmployeeContractResponse contract,
        List<EmployeePayrollItemSettingResponse> payrollItemSettings
) {
}
