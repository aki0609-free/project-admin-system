package com.project.backend.features.operation.monthly.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingResponse;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingExecution;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.mapper.MonthlyClosingMapper;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

import lombok.RequiredArgsConstructor;

/**
 * 月次締めの実行を制御するFacade。
 *
 * 業務データの確定Transactionと、実行状態を保存するTransactionを分離し、
 * 途中失敗時にもFAILEDの実行履歴を残す。
 */
@Service
@RequiredArgsConstructor
public class MonthlyClosingCommandService {

    private static final String SYSTEM_EXECUTOR = "SYSTEM";

    private final MonthlyClosingRepository monthlyClosingRepository;
    private final MonthlyClosingMapper mapper;
    private final MonthlyClosingWorkflowService workflowService;
    private final MonthlyClosingPeriodService periodService;
    private final MonthlyClosingOutputDefinitionService definitionService;
    private final MonthlyClosingExecutionStateService executionStateService;

    public MonthlyClosingResponse close(String targetMonthText) {
        return execute(targetMonthText, false);
    }

    public MonthlyClosingResponse reclose(String targetMonthText) {
        return execute(targetMonthText, true);
    }

    private MonthlyClosingResponse execute(
            String targetMonthText,
            boolean reclose
    ) {
        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);
        LocalDate monthStart = targetMonth.atDay(1);
        MonthlyClosing closing = findOrCreate(monthStart, reclose);
        validateExecution(closing, reclose);

        MonthlyClosingPeriod period = periodService.resolve(targetMonthText);
        applyPeriod(closing, period);
        closing.setNote(null);
        closing = monthlyClosingRepository.save(closing);

        List<MonthlyClosingOutputDefinition> definitions =
                definitionService.findActiveCompanyOutputs();
        if (definitions.isEmpty()) {
            throw new IllegalStateException(
                    "有効な自社月次締め帳票・台帳が設定されていません。"
            );
        }

        int nextVersion = executionStateService.nextVersion(
                closing.getId(),
                closing.getClosingVersion()
        );
        MonthlyClosingExecution execution = executionStateService.startNew(
                closing.getId(),
                nextVersion,
                currentUsername(),
                definitions
        );

        try {
            workflowService.execute(
                    closing.getId(),
                    period,
                    nextVersion,
                    definitions
            );
            executionStateService.completeItems(execution.getId());
            executionStateService.complete(execution.getId());
        } catch (RuntimeException | Error error) {
            recordFailure(execution.getId(), error);
            throw error;
        }

        MonthlyClosing completed = monthlyClosingRepository
                .findById(closing.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "完了した月次締めデータを取得できません。"
                ));
        return mapper.toResponse(completed);
    }

    private MonthlyClosing findOrCreate(
            LocalDate monthStart,
            boolean reclose
    ) {
        return monthlyClosingRepository
                .findByTargetMonthAndDeletedAtIsNull(monthStart)
                .orElseGet(() -> {
                    if (reclose) {
                        throw new IllegalStateException(
                                "月次締めデータがありません。"
                        );
                    }
                    MonthlyClosing created = new MonthlyClosing();
                    created.setTargetMonth(monthStart);
                    created.setStatus(MonthlyClosingStatus.OPEN);
                    created.setClosingVersion(0);
                    return created;
                });
    }

    private void validateExecution(
            MonthlyClosing closing,
            boolean reclose
    ) {
        if (closing.getStatus() == MonthlyClosingStatus.PROCESSING) {
            throw new IllegalStateException("月次締めを実行中です。");
        }
        int completedVersion = closing.getClosingVersion() != null
                ? closing.getClosingVersion()
                : 0;
        if (!reclose && (completedVersion > 0
                || closing.getStatus() == MonthlyClosingStatus.CLOSED)) {
            throw new IllegalStateException(
                    "既に月次締め済みです。再締めを実行してください。"
            );
        }
        if (reclose && completedVersion < 1) {
            throw new IllegalStateException(
                    "初回の月次締めが完了していません。"
            );
        }
    }

    private void recordFailure(Long executionId, Throwable original) {
        try {
            executionStateService.fail(executionId, original);
        } catch (RuntimeException stateError) {
            original.addSuppressed(stateError);
        }
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return SYSTEM_EXECUTOR;
        }
        return authentication.getName();
    }

    private void applyPeriod(
            MonthlyClosing entity,
            MonthlyClosingPeriod period
    ) {
        DayRule rule = period.rule();
        entity.setClosingStartDate(period.startDate());
        entity.setClosingEndDate(period.endDate());
        if (rule != null && rule.type() != null) {
            entity.setClosingRuleType(rule.type().name());
            entity.setClosingRuleValue(rule.value());
        }
    }
}
