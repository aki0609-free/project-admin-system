package com.project.backend.features.employee.service;

import org.springframework.stereotype.Component;

import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.repository.EmployeeLoanRepository;
import com.project.backend.features.employee.repository.EmployeeSavingRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.tax.repository.ResidentTaxMonthlyRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmployeeDeletionPolicy {

    private final DailyReportRepository dailyReportRepository;
    private final DailyPreparationAssignmentRepository preparationAssignmentRepository;
    private final EmployeeLoanRepository loanRepository;
    private final EmployeeSavingRepository savingRepository;
    private final ResidentTaxMonthlyRepository residentTaxRepository;

    public void verifyDeletable(Long employeeId) {
        if (dailyReportRepository.existsByEmployeeIdAndDeletedAtIsNull(employeeId)) {
            reject("日報");
        }
        if (preparationAssignmentRepository.existsByEmployeeIdAndDeletedAtIsNull(employeeId)) {
            reject("翌日準備");
        }
        if (loanRepository.existsByEmployeeIdAndDeletedAtIsNull(employeeId)) {
            reject("貸付");
        }
        if (savingRepository.existsByEmployeeIdAndDeletedAtIsNull(employeeId)) {
            reject("積立");
        }
        if (residentTaxRepository.existsByEmployeeIdAndDeletedAtIsNull(employeeId)) {
            reject("住民税");
        }
    }

    private void reject(String referenceName) {
        throw new IllegalStateException(
                referenceName + "から参照されているため削除できません。退職処理を使用してください。"
        );
    }
}
