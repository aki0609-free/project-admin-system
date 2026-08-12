package com.project.backend.features.operation.monthly.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.monthly.dto.CustomerBillingClosingResponse;
import com.project.backend.features.operation.monthly.entity.CustomerBillingClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.CustomerBillingClosingRepository;
import com.project.backend.features.operation.monthly.service.CustomerBillingTargetService.Target;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerBillingClosingExecutionService {

    private final CustomerBillingClosingRepository repository;
    private final CustomerBillingTargetService targetService;
    private final CustomerBillingClosingJobService jobService;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CustomerBillingClosingResponse execute(
            String targetMonthText,
            Long customerId,
            boolean reclose
    ) {
        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);
        LocalDate monthStart = targetMonth.atDay(1);
        Target target = targetService.findTarget(targetMonthText, customerId);
        CustomerBillingClosing closing = repository
                .findByTargetMonthAndCustomerIdAndDeletedAtIsNull(
                        monthStart,
                        customerId
                )
                .orElseGet(() -> {
                    CustomerBillingClosing created = new CustomerBillingClosing();
                    created.setTargetMonth(monthStart);
                    created.setCustomerId(customerId);
                    return created;
                });

        if (!reclose && closing.getStatus() == MonthlyClosingStatus.CLOSED) {
            throw new IllegalStateException(
                    "既に顧客請求締め済みです。再締めを実行してください。"
            );
        }
        if (reclose && closing.getId() == null) {
            throw new IllegalStateException("顧客請求締めデータがありません。");
        }

        closing = repository.save(closing);
        int nextVersion = closing.getClosingVersion() + 1;
        jobService.execute(
                closing.getId(),
                targetMonthText,
                nextVersion,
                target
        );

        closing.setClosingVersion(nextVersion);
        closing.setStatus(MonthlyClosingStatus.CLOSED);
        closing.setClosedAt(Instant.now(clock));
        return toResponse(repository.save(closing));
    }

    private CustomerBillingClosingResponse toResponse(
            CustomerBillingClosing closing
    ) {
        return new CustomerBillingClosingResponse(
                closing.getId(),
                closing.getTargetMonth(),
                closing.getCustomerId(),
                closing.getStatus().name(),
                closing.getClosingVersion(),
                closing.getClosedAt()
        );
    }
}
