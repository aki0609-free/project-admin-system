package com.project.backend.features.employee.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.employee.entity.EmployeeFinanceTransaction;
import com.project.backend.features.employee.enums.EmployeeFinanceAccountType;

public interface EmployeeFinanceTransactionRepository
        extends JpaRepository<EmployeeFinanceTransaction, Long> {

    List<EmployeeFinanceTransaction>
            findAllByDeletedAtIsNullOrderByTransactionDateDescIdDesc();

    List<EmployeeFinanceTransaction>
            findAllByEmployeeIdAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                    Long employeeId
            );

    List<EmployeeFinanceTransaction>
            findAllByAccountTypeAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                    EmployeeFinanceAccountType accountType
            );

    List<EmployeeFinanceTransaction>
            findAllByEmployeeIdAndAccountTypeAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                    Long employeeId,
                    EmployeeFinanceAccountType accountType
            );
}
