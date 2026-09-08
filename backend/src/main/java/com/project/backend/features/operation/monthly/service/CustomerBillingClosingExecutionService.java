package com.project.backend.features.operation.monthly.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

    private static final String SYSTEM_EXECUTOR = "SYSTEM";

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
        int completedVersion = closing.getClosingVersion() == null
                ? 0
                : closing.getClosingVersion();
        if (reclose && (closing.getId() == null
                || closing.getStatus() != MonthlyClosingStatus.CLOSED
                || completedVersion < 1)) {
            throw new IllegalStateException(
                    "初回の顧客請求締めが完了していません。"
            );
        }

        closing = repository.save(closing);
        int nextVersion = completedVersion + 1;
        jobService.execute(
                closing.getId(),
                targetMonthText,
                nextVersion,
                target
        );

        closing.setClosingVersion(nextVersion);
        closing.setStatus(MonthlyClosingStatus.CLOSED);
        closing.setClosedAt(Instant.now(clock));
        closing.setClosedBy(currentUsername());
        return toResponse(repository.save(closing));
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
