package com.project.backend.features.operation.monthly.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingResponse;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.mapper.MonthlyClosingMapper;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyClosingCommandService {

    private final MonthlyClosingRepository monthlyClosingRepository;
    private final MonthlyClosingMapper mapper;
    private final MonthlyClosingJobService monthlyClosingJobService;
    private final MonthlyClosingPeriodService monthlyClosingPeriodService;

    public MonthlyClosingResponse close(String targetMonthText) {
        YearMonth targetMonth = MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);
        LocalDate monthStart = targetMonth.atDay(1);

        MonthlyClosing entity = monthlyClosingRepository
                                .findByTargetMonthAndDeletedAtIsNull(monthStart)
                                .orElseGet(() -> {
                                    MonthlyClosing created = new MonthlyClosing();
                                    created.setTargetMonth(monthStart);
                                    created.setStatus(MonthlyClosingStatus.OPEN);
                                    created.setClosingVersion(0);
                                    return created;
                                });

        if (entity.getStatus() == MonthlyClosingStatus.CLOSED) {
            throw new RuntimeException("既に月次締め済みです。再締めを実行してください。");
        }

        MonthlyClosingPeriod period =
                monthlyClosingPeriodService.resolve(targetMonthText);

        applyPeriod(entity, period);

        entity =
                monthlyClosingRepository.save(entity);

        Integer nextVersion =
                entity.getClosingVersion() + 1;

        monthlyClosingJobService.executeClosing(
                entity.getId(),
                period,
                nextVersion
        );

        entity.setClosingVersion(nextVersion);
        entity.setStatus(MonthlyClosingStatus.CLOSED);
        entity.setClosedAt(Instant.now());

        entity =
                monthlyClosingRepository.save(entity);

        return mapper.toResponse(entity);
    }

    @SuppressWarnings("null")
    public MonthlyClosingResponse reclose(String targetMonthText) {
        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);

        LocalDate monthStart =
                targetMonth.atDay(1);

        MonthlyClosing entity =
                monthlyClosingRepository
                        .findByTargetMonthAndDeletedAtIsNull(monthStart)
                        .orElseThrow(() ->
                                new RuntimeException("月次締めデータがありません。"));

        MonthlyClosingPeriod period =
                monthlyClosingPeriodService.resolve(targetMonthText);

        applyPeriod(entity, period);

        entity =
                monthlyClosingRepository.save(entity);

        Integer nextVersion =
                entity.getClosingVersion() + 1;

        monthlyClosingJobService.executeClosing(
                entity.getId(),
                period,
                nextVersion
        );

        entity.setClosingVersion(nextVersion);
        entity.setStatus(MonthlyClosingStatus.CLOSED);
        entity.setClosedAt(Instant.now());

        entity =
                monthlyClosingRepository.save(entity);

        return mapper.toResponse(entity);
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