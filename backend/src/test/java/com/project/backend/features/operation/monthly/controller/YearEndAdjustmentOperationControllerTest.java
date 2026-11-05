package com.project.backend.features.operation.monthly.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.features.admin.business.dto.YearEndAdjustmentResponse;
import com.project.backend.features.admin.business.dto.YearEndAdjustmentSaveRequest;
import com.project.backend.features.admin.business.service.YearEndAdjustmentService;

@ExtendWith(MockitoExtension.class)
class YearEndAdjustmentOperationControllerTest {

    @Mock
    private YearEndAdjustmentService service;

    private YearEndAdjustmentOperationController controller;

    @BeforeEach
    void setUp() {
        controller = new YearEndAdjustmentOperationController(service);
    }

    @Test
    void delegatesYearQueryToExistingBusinessService() {
        List<YearEndAdjustmentResponse> expected = List.of(response());
        when(service.findByYear(2026)).thenReturn(expected);

        assertSame(expected, controller.findByYear(2026));
    }

    @Test
    void delegatesSaveAndDeleteToExistingBusinessService() {
        YearEndAdjustmentSaveRequest request = new YearEndAdjustmentSaveRequest(
                null,
                10L,
                2026,
                YearMonth.of(2026, 12),
                BigDecimal.valueOf(12000),
                "DRAFT",
                null,
                null
        );
        YearEndAdjustmentResponse expected = response();
        when(service.save(request)).thenReturn(expected);

        assertSame(expected, controller.save(request));
        controller.delete(1L);

        verify(service).delete(1L);
    }

    private YearEndAdjustmentResponse response() {
        return new YearEndAdjustmentResponse(
                1L,
                10L,
                "E001",
                "検証 太郎",
                2026,
                YearMonth.of(2026, 12),
                BigDecimal.valueOf(12000),
                "DRAFT",
                null,
                0L
        );
    }
}
