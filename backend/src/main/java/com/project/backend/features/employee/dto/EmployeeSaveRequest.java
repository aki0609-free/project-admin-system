package com.project.backend.features.employee.dto;

import java.time.LocalDate;

import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.EmploymentType;
import com.project.backend.features.employee.enums.Gender;

public record EmployeeSaveRequest(
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
        Boolean activeFlag,
        EmployeePayrollProfileSaveRequest payrollProfile,
        EmployeeContractSaveRequest contract
) {
}