package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookLayoutType;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

class SpreadsheetLedgerEditingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExcelBookMasterRepository masterRepository;
    private MonthlyClosingRepository closingRepository;
    private StorageService storageService;
    private MonthlySummarySpreadsheetRenderer renderer;
    private SpreadsheetLedgerEditingService service;
    private ExcelBookMaster master;
    private JsonNode workbook;

    @BeforeEach
    void setUp() throws Exception {
        masterRepository = mock(ExcelBookMasterRepository.class);
        closingRepository = mock(MonthlyClosingRepository.class);
        storageService = mock(StorageService.class);
        renderer = mock(
                MonthlySummarySpreadsheetRenderer.class
        );
        when(renderer.rendererKey()).thenReturn(
                MonthlySummarySpreadsheetRenderer.KEY
        );
        when(renderer.editableBeforeClosing()).thenReturn(true);
        when(renderer.usesStableMonthlyPath()).thenReturn(true);
        service = new SpreadsheetLedgerEditingService(
                masterRepository,
                new SpreadsheetLedgerRendererRegistry(
                        List.of(renderer)
                ),
                new SpreadsheetLedgerEditHandlerRegistry(List.of()),
                closingRepository,
                storageService,
                new DocumentStorageKeyResolver(
                        new StorageProperties()
                ),
                objectMapper,
                Clock.fixed(
                        Instant.parse("2026-07-29T12:00:00Z"),
                        ZoneId.of("Asia/Tokyo")
                )
        );

        master = new ExcelBookMaster();
        master.setBookCode("MONTHLY_SUMMARY");
        master.setTenantId("default");
        master.setLayoutType(
                ExcelBookLayoutType.MONTHLY_SUMMARY
        );
        workbook = objectMapper.readTree(
                """
                {"Workbook":{"sheets":[{"name":"2026.07"}]}}
                """
        );
        when(masterRepository
                .findFirstByBookCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        "MONTHLY_SUMMARY"
                ))
                .thenReturn(Optional.of(master));
    }

    @Test
    void save_shouldOverwriteMonthlyJsonBeforeClosing()
            throws Exception {
        when(closingRepository
                .findByTargetMonthAndDeletedAtIsNull(
                        LocalDate.of(2026, 7, 1)
                ))
                .thenReturn(Optional.empty());

        var result = service.save(
                "MONTHLY_SUMMARY",
                "2026-07",
                workbook
        );

        assertThat(result.storagePath()).isEqualTo(
                "ledgers/default/MONTHLY_SUMMARY/2026-07/"
                        + "MONTHLY_SUMMARY-2026-07.json"
        );
        assertThat(result.savedAt()).isEqualTo(
                Instant.parse("2026-07-29T12:00:00Z")
        );
        verify(storageService).save(
                eq("documents/generated-reports/"
                        + result.storagePath()),
                any(InputStream.class),
                eq((long) result.workbookBytes()),
                eq("application/json")
        );
    }

    @Test
    void save_shouldRejectClosedMonth() {
        MonthlyClosing closing = new MonthlyClosing();
        closing.setStatus(MonthlyClosingStatus.CLOSED);
        when(closingRepository
                .findByTargetMonthAndDeletedAtIsNull(
                        LocalDate.of(2026, 7, 1)
                ))
                .thenReturn(Optional.of(closing));

        assertThatThrownBy(() -> service.save(
                "MONTHLY_SUMMARY",
                "2026-07",
                workbook
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("締め済み");

        verify(storageService, never()).save(
                any(),
                any(),
                any(Long.class),
                any()
        );
    }

    @Test
    void save_shouldAllowClosedMonthWhenRendererExplicitlySupportsIt()
            throws Exception {
        MonthlyClosing closing = new MonthlyClosing();
        closing.setStatus(MonthlyClosingStatus.CLOSED);
        when(closingRepository
                .findByTargetMonthAndDeletedAtIsNull(
                        LocalDate.of(2026, 7, 1)
                ))
                .thenReturn(Optional.of(closing));
        when(renderer.editableAfterMonthlyClosing()).thenReturn(true);

        var result = service.save(
                "MONTHLY_SUMMARY",
                "2026-07",
                workbook
        );

        assertThat(result.storagePath()).contains("2026-07");
        verify(storageService).save(
                eq("documents/generated-reports/"
                        + result.storagePath()),
                any(InputStream.class),
                eq((long) result.workbookBytes()),
                eq("application/json")
        );
    }
}
