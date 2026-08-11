package com.project.backend.features.master.payrollitem.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePayrollItemTransactionRepository
        extends JpaRepository<EmployeePayrollItemTransaction, Long> {

    List<EmployeePayrollItemTransaction>
            findAllByTenantIdAndEmployeeIdAndTargetCodeAndTargetMonthAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                    String tenantId,
                    Long employeeId,
                    String targetCode,
                    LocalDate targetMonth
            );

    Optional<EmployeePayrollItemTransaction>
            findByIdAndTenantIdAndEmployeeIdAndDeletedAtIsNull(
                    Long id,
                    String tenantId,
                    Long employeeId
            );
}
