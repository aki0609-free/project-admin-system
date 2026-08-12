package com.project.backend.features.operation.monthly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.LegalDepositRefund;
import com.project.backend.features.operation.monthly.enums.LegalDepositRefundStatus;

public interface LegalDepositRefundRepository
        extends JpaRepository<LegalDepositRefund, Long> {

    List<LegalDepositRefund>
            findByMonthlyClosingIdAndStatusAndDeletedAtIsNull(
                    Long monthlyClosingId,
                    LegalDepositRefundStatus status
            );
}
