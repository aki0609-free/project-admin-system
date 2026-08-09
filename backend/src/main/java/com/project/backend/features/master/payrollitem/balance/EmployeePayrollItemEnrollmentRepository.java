package com.project.backend.features.master.payrollitem.balance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePayrollItemEnrollmentRepository
        extends JpaRepository<EmployeePayrollItemEnrollment, Long> {

    List<EmployeePayrollItemEnrollment>
            findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                    Long employeeId,
                    Long balancePolicyId,
                    LocalDate through
            );

    Optional<EmployeePayrollItemEnrollment>
            findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                    Long employeeId,
                    Long balancePolicyId
            );

    List<EmployeePayrollItemEnrollment>
            findAllByEmployeeIdAndDeletedAtIsNullOrderByEffectiveFromAsc(Long employeeId);
}
