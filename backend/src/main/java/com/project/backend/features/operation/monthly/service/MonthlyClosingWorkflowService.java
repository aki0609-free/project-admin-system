package com.project.backend.features.operation.monthly.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;

import lombok.RequiredArgsConstructor;

/**
 * 月次締めVersionの業務データ・帳票・台帳を同一Transactionで確定する。
 * 実行状態はMonthlyClosingExecutionStateServiceが別Transactionで管理する。
 */
@Service
@RequiredArgsConstructor
public class MonthlyClosingWorkflowService {

    private final LegalDepositRefundService legalDepositRefundService;
    private final MonthlyClosingJobService monthlyClosingJobService;

    @Transactional
    public void execute(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion,
            List<MonthlyClosingOutputDefinition> definitions
    ) {
        legalDepositRefundService.prepareRefunds(
                monthlyClosingId,
                period,
                closingVersion
        );
        monthlyClosingJobService.executeClosing(
                monthlyClosingId,
                period,
                closingVersion,
                definitions
        );
    }
}
