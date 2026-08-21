package com.project.backend.features.master.payrollitem.transaction;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public interface EmployeePayrollItemTransactionRepository
        extends JpaRepository<EmployeePayrollItemTransaction, Long> {

    List<EmployeePayrollItemTransaction>
            findAllByTenantIdAndEmployeeIdAndTargetTypeAndTargetCodeAndTargetMonthAndDeletedAtIsNullOrderByTransactionDateDescIdDesc(
                    String tenantId,
                    Long employeeId,
                    PayrollItemTargetType targetType,
                    String targetCode,
                    LocalDate targetMonth
            );

    Optional<EmployeePayrollItemTransaction>
            findByIdAndTenantIdAndEmployeeIdAndDeletedAtIsNull(
                    Long id,
                    String tenantId,
                    Long employeeId
            );

    @Query(value = """
            SELECT COALESCE(SUM(item.quantity), 0)
            FROM employee_payroll_item_transaction item
            WHERE item.tenant_id = :tenantId
              AND item.employee_id = :employeeId
              AND item.target_type = :targetType
              AND item.target_master_id = :masterId
              AND item.balance_effect = :balanceEffect
              AND item.status = 'CONFIRMED'
              AND item.transaction_date BETWEEN :from AND :through
              AND item.deleted_at IS NULL
            """, nativeQuery = true)
    BigDecimal sumConfirmedQuantityByEffect(
            @Param("tenantId") String tenantId,
            @Param("employeeId") Long employeeId,
            @Param("targetType") String targetType,
            @Param("masterId") Long masterId,
            @Param("balanceEffect") String balanceEffect,
            @Param("from") LocalDate from,
            @Param("through") LocalDate through
    );
}
