package com.project.backend.features.operation.monthly.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.backend.features.operation.monthly.dto.CustomerBillingBulkClosingResponse;
import com.project.backend.features.operation.monthly.dto.CustomerBillingClosingResponse;
import com.project.backend.features.operation.monthly.entity.CustomerBillingClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.CustomerBillingClosingRepository;
import com.project.backend.features.operation.monthly.service.CustomerBillingTargetService.Target;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerBillingClosingCommandService {

    private final CustomerBillingClosingRepository repository;
    private final CustomerBillingTargetService targetService;
    private final CustomerBillingClosingExecutionService executionService;
    private final Clock clock;

    public CustomerBillingClosingResponse close(
            String targetMonth,
            Long customerId
    ) {
        return executionService.execute(targetMonth, customerId, false);
    }

    public CustomerBillingClosingResponse reclose(
            String targetMonth,
            Long customerId
    ) {
        return executionService.execute(targetMonth, customerId, true);
    }

    /**
     * 実行日が顧客の締日以後で、まだ締めていない顧客だけを個別Transactionで締める。
     */
    public CustomerBillingBulkClosingResponse closeAllEligible(
            String targetMonthText
    ) {
        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);
        LocalDate today = LocalDate.now(clock);
        List<Target> targets = targetService.findTargets(targetMonthText);
        List<Long> customerIds = targets.stream()
                .map(target -> target.customer().getId())
                .toList();
        Map<Long, CustomerBillingClosing> existingByCustomerId = customerIds.isEmpty()
                ? Map.of()
                : repository
                        .findAllByTargetMonthAndCustomerIdInAndDeletedAtIsNull(
                                targetMonth.atDay(1),
                                customerIds
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                CustomerBillingClosing::getCustomerId,
                                Function.identity()
                        ));

        int completed = 0;
        int skippedBeforeClosingDate = 0;
        int alreadyClosed = 0;
        List<String> errors = new ArrayList<>();

        for (Target target : targets) {
            if (today.isBefore(target.period().endDate())) {
                skippedBeforeClosingDate++;
                continue;
            }
            CustomerBillingClosing existing = existingByCustomerId.get(
                    target.customer().getId()
            );
            if (existing != null
                    && existing.getStatus() == MonthlyClosingStatus.CLOSED) {
                alreadyClosed++;
                continue;
            }
            try {
                executionService.execute(
                        targetMonthText,
                        target.customer().getId(),
                        false
                );
                completed++;
            } catch (RuntimeException exception) {
                errors.add(target.customer().getName() + ": " + exception.getMessage());
            }
        }

        return new CustomerBillingBulkClosingResponse(
                targets.size(),
                completed,
                skippedBeforeClosingDate,
                alreadyClosed,
                errors.size(),
                List.copyOf(errors)
        );
    }
}
