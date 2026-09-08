package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingReportFileRepository;

class MonthlyClosingReportFileServiceTest {

    @Test
    void latestCustomerBillingFilesCanBeLimitedToSelectedCustomer() {
        MonthlyClosingReportFileRepository repository =
                mock(MonthlyClosingReportFileRepository.class);
        MonthlyClosingReportFile customerA = file(1L, 10L);
        MonthlyClosingReportFile customerB = file(2L, 20L);
        when(repository
                .findAllByTargetMonthAndClosingScopeAndDeletedAtIsNullOrderByIdDesc(
                        "2026-09",
                        "CUSTOMER_BILLING"
                )).thenReturn(List.of(customerB, customerA));
        MonthlyClosingReportFileService service =
                new MonthlyClosingReportFileService(repository);

        var files = service.findAll(
                "2026-09",
                null,
                "MONTHLY_INVOICE",
                10L
        );

        assertThat(files)
                .extracting(file -> file.targetId())
                .containsExactly(10L);
    }

    private MonthlyClosingReportFile file(Long id, Long customerId) {
        MonthlyClosingReportFile file = new MonthlyClosingReportFile();
        file.setId(id);
        file.setTargetId(customerId);
        file.setReportCode("MONTHLY_INVOICE_PATTERN_1");
        file.setTargetName("顧客" + customerId);
        return file;
    }
}
