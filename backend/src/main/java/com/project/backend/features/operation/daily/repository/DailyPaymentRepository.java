package com.project.backend.features.operation.daily.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.daily.entity.DailyPayment;

public interface DailyPaymentRepository extends JpaRepository<DailyPayment, Long> {

        List<DailyPayment> findByPaymentDateAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(
                        LocalDate paymentDate);

        Optional<DailyPayment> findByIdAndDeletedAtIsNull(Long id);

        Optional<DailyPayment> findByPaymentDateAndEmployeeIdAndDeletedAtIsNull(
                        LocalDate paymentDate,
                        Long employeeId);

        List<DailyPayment> findByPaymentDateBetweenAndDeletedAtIsNullOrderByPaymentDateAscEmployeeCodeAscIdAsc(
                        LocalDate from,
                        LocalDate to);
}