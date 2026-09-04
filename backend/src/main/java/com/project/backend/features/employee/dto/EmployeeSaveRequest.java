package com.project.backend.features.employee.dto;

import java.time.LocalDate;
import java.util.List;

import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.EmploymentType;
import com.project.backend.features.employee.enums.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeSaveRequest(
        @NotBlank @Size(max = 100) String employeeCode,
        @NotBlank @Size(max = 200) String employeeName,
        @Size(max = 200) String employeeNameKana,
        Gender gender,
        LocalDate birthDate,
        LocalDate hireDate,
        LocalDate resignDate,
        @NotNull EmploymentType employmentType,
        @NotNull EmploymentStatus employmentStatus,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String postalCode,
        @Size(max = 500) String address,
        Boolean activeFlag,
        @Valid EmployeePayrollProfileSaveRequest payrollProfile,
        @Valid EmployeeContractSaveRequest contract,
        @Valid List<EmployeePayrollItemSettingRequest> payrollItemSettings
) {
}
