package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.dto.DailyReportAllowanceSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.repository.DailyReportAllowanceRepository;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.master.payrollitem.service.PayrollMoneyPolicy;

class DailyReportPayrollItemAmountValidationTest {

    @Test
    void allowanceSave_shouldRejectNegativeAmountBeforeReplacingExistingRows() {
        DailyReportAllowanceRepository repository =
                mock(DailyReportAllowanceRepository.class);
        DailyReportAllowanceCommandService service =
                new DailyReportAllowanceCommandService(
                        repository,
                        new PayrollMoneyPolicy()
                );

        assertThatThrownBy(() -> service.replaceAll(
                1L,
                List.of(new DailyReportAllowanceSaveRequest(
                        10L,
                        "ADJUSTMENT_ALLOWANCE",
                        "調整手当",
                        100,
                        -1,
                        true,
                        "負数入力",
                        null,
                        null
                ))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("手当金額は0以上で指定してください。");
        verifyNoInteractions(repository);
    }

    @Test
    void deductionSave_shouldRejectNegativeCalculatedAmountBeforeReplacingRows() {
        DailyReportDeductionRepository repository =
                mock(DailyReportDeductionRepository.class);
        DailyReportDeductionCommandService service =
                new DailyReportDeductionCommandService(
                        repository,
                        new PayrollMoneyPolicy()
                );

        assertThatThrownBy(() -> service.replaceAll(
                1L,
                List.of(new DailyReportDeductionSaveRequest(
                        20L,
                        "TEST_DEDUCTION",
                        "テスト控除",
                        -1,
                        0,
                        false,
                        null,
                        null,
                        null
                ))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("控除の計算額は0以上で指定してください。");
        verifyNoInteractions(repository);
    }
}
