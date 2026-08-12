package com.project.backend.features.operation.monthly.service;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingReportFileResponse;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingReportFileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonthlyClosingReportFileService {

    private static final String MONTHLY_INVOICE_CODE = "MONTHLY_INVOICE";
    private static final String MONTHLY_INVOICE_PATTERN_PREFIX = "MONTHLY_INVOICE_PATTERN_";

    private final MonthlyClosingReportFileRepository repository;

    @Transactional(readOnly = true)
    public List<MonthlyClosingReportFileResponse> findAll(
            String targetMonth,
            Integer closingVersion,
            String reportCode) {
        if (targetMonth == null || !targetMonth.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("targetMonthはyyyy-MM形式で指定してください。");
        }
        if (closingVersion != null && closingVersion < 1) {
            throw new IllegalArgumentException("closingVersionは1以上で指定してください。");
        }
        if (reportCode == null || reportCode.isBlank()) {
            throw new IllegalArgumentException("reportCodeは必須です。");
        }

        List<MonthlyClosingReportFile> files = closingVersion == null
                ? findLatestCustomerBillingFiles(targetMonth)
                : repository
                        .findAllByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByTargetNameAscIdAsc(
                                targetMonth,
                                closingVersion
                        );
        return files
                .stream()
                .filter(file -> matchesReportCode(file.getReportCode(), reportCode))
                .map(this::toResponse)
                .toList();
    }

    private List<MonthlyClosingReportFile> findLatestCustomerBillingFiles(
            String targetMonth
    ) {
        Set<String> latestTargets = new HashSet<>();
        return repository
                .findAllByTargetMonthAndClosingScopeAndDeletedAtIsNullOrderByIdDesc(
                        targetMonth,
                        "CUSTOMER_BILLING"
                )
                .stream()
                .filter(file -> latestTargets.add(
                        file.getReportCode() + ":" + file.getTargetId()
                ))
                .toList();
    }

    private boolean matchesReportCode(String storedReportCode, String requestedReportCode) {
        if (MONTHLY_INVOICE_CODE.equals(requestedReportCode)) {
            return storedReportCode != null
                    && storedReportCode.startsWith(MONTHLY_INVOICE_PATTERN_PREFIX);
        }
        return requestedReportCode.equals(storedReportCode);
    }

    private MonthlyClosingReportFileResponse toResponse(MonthlyClosingReportFile file) {
        return new MonthlyClosingReportFileResponse(
                file.getId(),
                file.getReportCode(),
                file.getTargetType(),
                file.getTargetId(),
                file.getTargetName(),
                file.getBatchExecutionLogId(),
                file.getStorageType() == null ? null : file.getStorageType().name(),
                file.getOutputFileKey(),
                file.getOutputFileName(),
                file.getContentType(),
                file.getFileSize(),
                file.getGeneratedAt());
    }
}
