package com.project.backend.features.operation.monthly.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingExecution;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingItem;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingExecutionStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingItemStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingExecutionRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingItemRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonthlyClosingExecutionStateService {

    private static final int MAX_ERROR_LENGTH = 4000;

    private final MonthlyClosingRepository closingRepository;
    private final MonthlyClosingExecutionRepository executionRepository;
    private final MonthlyClosingItemRepository itemRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MonthlyClosingExecution startNew(
            Long monthlyClosingId,
            Integer closingVersion,
            String executedBy,
            List<MonthlyClosingOutputDefinition> definitions
    ) {
        if (closingVersion == null || closingVersion < 1) {
            throw new IllegalArgumentException(
                    "closingVersionは1以上で指定してください。"
            );
        }
        if (!StringUtils.hasText(executedBy)) {
            throw new IllegalArgumentException(
                    "締め処理の実行者は必須です。"
            );
        }
        boolean versionInUse = executionRepository
                .existsByMonthlyClosingIdAndClosingVersionAndStatusInAndDeletedAtIsNull(
                        monthlyClosingId,
                        closingVersion,
                        List.of(
                                MonthlyClosingExecutionStatus.PROCESSING,
                                MonthlyClosingExecutionStatus.COMPLETED
                        )
                );
        if (versionInUse) {
            throw new IllegalStateException(
                    "指定Versionの締め処理は実行中または完了済みです。"
            );
        }

        Instant now = Instant.now(clock);
        MonthlyClosing closing = findClosing(monthlyClosingId);
        closing.setStatus(MonthlyClosingStatus.PROCESSING);
        closingRepository.save(closing);

        MonthlyClosingExecution execution =
                new MonthlyClosingExecution();
        execution.setMonthlyClosingId(monthlyClosingId);
        execution.setClosingVersion(closingVersion);
        execution.setStatus(
                MonthlyClosingExecutionStatus.PROCESSING
        );
        execution.setStartedAt(now);
        execution.setExecutedBy(executedBy);
        execution = executionRepository.save(execution);

        for (MonthlyClosingOutputDefinition definition
                : safeDefinitions(definitions)) {
            MonthlyClosingItem item = new MonthlyClosingItem();
            item.setMonthlyClosingExecutionId(execution.getId());
            item.setOutputType(definition.getOutputType());
            item.setOutputCode(definition.getOutputCode());
            item.setTargetKey("ALL");
            item.setRequiredFlag(definition.getRequiredFlag());
            item.setStatus(MonthlyClosingItemStatus.WAITING);
            itemRepository.save(item);
        }
        return execution;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long executionId) {
        Instant now = Instant.now(clock);
        MonthlyClosingExecution execution =
                findExecution(executionId);
        ensureNoRequiredFailure(executionId);

        execution.setStatus(
                MonthlyClosingExecutionStatus.COMPLETED
        );
        execution.setCompletedAt(now);
        execution.setErrorMessage(null);
        executionRepository.save(execution);

        MonthlyClosing closing =
                findClosing(execution.getMonthlyClosingId());
        closing.setStatus(MonthlyClosingStatus.CLOSED);
        closing.setClosingVersion(execution.getClosingVersion());
        closing.setClosedAt(now);
        closing.setClosedBy(execution.getExecutedBy());
        closingRepository.save(closing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeItems(Long executionId) {
        Instant now = Instant.now(clock);
        List<MonthlyClosingItem> items = itemRepository
                .findByMonthlyClosingExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                        executionId
                );
        for (MonthlyClosingItem item : items) {
            if (item.getStatus() == MonthlyClosingItemStatus.COMPLETED) {
                continue;
            }
            item.setStatus(MonthlyClosingItemStatus.COMPLETED);
            if (item.getStartedAt() == null) {
                item.setStartedAt(now);
            }
            item.setCompletedAt(now);
            item.setErrorMessage(null);
        }
        itemRepository.saveAll(items);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long executionId, Throwable error) {
        Instant now = Instant.now(clock);
        String message = limitError(error);
        MonthlyClosingExecution execution =
                findExecution(executionId);
        List<MonthlyClosingItem> items = itemRepository
                .findByMonthlyClosingExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                        executionId
                );
        for (MonthlyClosingItem item : items) {
            if (item.getStatus() == MonthlyClosingItemStatus.COMPLETED) {
                continue;
            }
            item.setStatus(MonthlyClosingItemStatus.FAILED);
            if (item.getStartedAt() == null) {
                item.setStartedAt(now);
            }
            item.setCompletedAt(now);
            item.setErrorMessage(message);
        }
        itemRepository.saveAll(items);
        execution.setStatus(MonthlyClosingExecutionStatus.FAILED);
        execution.setCompletedAt(now);
        execution.setErrorMessage(message);
        executionRepository.save(execution);

        MonthlyClosing closing =
                findClosing(execution.getMonthlyClosingId());
        closing.setStatus(MonthlyClosingStatus.FAILED);
        closing.setNote(message);
        closingRepository.save(closing);
    }

    @Transactional(readOnly = true)
    public int nextVersion(Long monthlyClosingId, Integer completedVersion) {
        int latestCompletedVersion = completedVersion != null
                ? completedVersion
                : 0;
        return latestCompletedVersion + 1;
    }

    private void ensureNoRequiredFailure(Long executionId) {
        boolean failed = itemRepository
                .findByMonthlyClosingExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                        executionId
                )
                .stream()
                .anyMatch(item ->
                        Boolean.TRUE.equals(item.getRequiredFlag())
                                && item.getStatus()
                                != MonthlyClosingItemStatus.COMPLETED
                );
        if (failed) {
            throw new IllegalStateException(
                    "必須の月次締め項目が完了していません。"
            );
        }
    }

    private MonthlyClosing findClosing(Long id) {
        return closingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "月次締めデータが見つかりません: " + id
                ));
    }

    private MonthlyClosingExecution findExecution(Long id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "月次締め実行が見つかりません: " + id
                ));
    }

    private List<MonthlyClosingOutputDefinition> safeDefinitions(
            List<MonthlyClosingOutputDefinition> definitions
    ) {
        return definitions != null ? definitions : List.of();
    }

    private String limitError(Throwable error) {
        String technicalMessage = errorChainMessage(error);
        String message;
        if (technicalMessage.contains(
                "monthly pay slip calculation master is not ready"
        )) {
            message = "月次給与明細の計算マスターが未準備です。"
                    + "対象月の税・社会保険計算期間を確認してください。";
        } else if (technicalMessage.contains(
                "Errors were encountered when compiling report expressions"
        ) || technicalMessage.contains("JRException")) {
            message = "帳票テンプレートの生成に失敗しました。"
                    + "帳票テンプレートを確認してください。";
        } else {
            message = error != null && StringUtils.hasText(error.getMessage())
                    ? error.getMessage()
                    : "月次締め処理に失敗しました。";
        }
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }

    private String errorChainMessage(Throwable error) {
        StringBuilder messages = new StringBuilder();
        Throwable current = error;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                messages.append(current.getMessage()).append('\n');
            }
            current = current.getCause();
        }
        return messages.toString();
    }
}
