package com.project.backend.features.employee.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeFinanceTransactionResponse;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeFinanceTransaction;
import com.project.backend.features.employee.enums.EmployeeFinanceAccountType;
import com.project.backend.features.employee.enums.EmployeeFinanceTransactionType;
import com.project.backend.features.employee.repository.EmployeeFinanceTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeFinanceTransactionService {

    private final EmployeeFinanceTransactionRepository repository;

    @Transactional
    public void record(
            Employee employee,
            EmployeeFinanceAccountType accountType,
            EmployeeFinanceTransactionType transactionType,
            Long accountReferenceId,
            Long dailyReportId,
            LocalDate transactionDate,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String note
    ) {
        BigDecimal before = nvl(balanceBefore);
        BigDecimal after = nvl(balanceAfter);
        if (before.compareTo(after) == 0) {
            return;
        }

        EmployeeFinanceTransaction entity = new EmployeeFinanceTransaction();
        entity.setEmployee(employee);
        entity.setAccountType(accountType);
        entity.setTransactionType(transactionType);
        entity.setAccountReferenceId(accountReferenceId);
        entity.setDailyReportId(dailyReportId);
        entity.setTransactionDate(transactionDate);
        entity.setAmount(after.subtract(before));
        entity.setBalanceBefore(before);
        entity.setBalanceAfter(after);
        entity.setNote(note);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<EmployeeFinanceTransactionResponse> findAll(
            Long employeeId,
            EmployeeFinanceAccountType accountType
    ) {
        List<EmployeeFinanceTransaction> entities;
        if (employeeId != null && accountType != null) {
            entities = repository
                    .findAllByEmployeeIdAndAccountTypeAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                            employeeId, accountType
                    );
        } else if (employeeId != null) {
            entities = repository
                    .findAllByEmployeeIdAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(employeeId);
        } else if (accountType != null) {
            entities = repository
                    .findAllByAccountTypeAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(accountType);
        } else {
            entities = repository.findAllByDeletedAtIsNullOrderByTransactionDateDescIdDesc();
        }
        return entities.stream().map(this::toResponse).toList();
    }

    private EmployeeFinanceTransactionResponse toResponse(
            EmployeeFinanceTransaction entity
    ) {
        Employee employee = entity.getEmployee();
        return EmployeeFinanceTransactionResponse.builder()
                .id(entity.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getEmployeeName())
                .accountType(entity.getAccountType())
                .transactionType(entity.getTransactionType())
                .accountReferenceId(entity.getAccountReferenceId())
                .dailyReportId(entity.getDailyReportId())
                .transactionDate(entity.getTransactionDate())
                .amount(entity.getAmount())
                .balanceBefore(entity.getBalanceBefore())
                .balanceAfter(entity.getBalanceAfter())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
