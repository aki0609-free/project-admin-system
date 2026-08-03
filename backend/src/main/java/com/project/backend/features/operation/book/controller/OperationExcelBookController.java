package com.project.backend.features.operation.book.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.operation.book.dto.OperationExcelBookResponse;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerGenerateRequest;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerGenerateResponse;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerSaveRequest;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerSaveResponse;
import com.project.backend.features.operation.book.dto.SpreadsheetLedgerSelectionResponse;
import com.project.backend.features.operation.book.service.SpreadsheetLedgerEditingService;
import com.project.backend.features.operation.book.service.SpreadsheetLedgerGenerationService;
import com.project.backend.features.operation.book.service.SpreadsheetLedgerSelectionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operation/excel-books")
@RequiredArgsConstructor
@Validated
public class OperationExcelBookController {

    private final SpreadsheetLedgerGenerationService service;
    private final SpreadsheetLedgerEditingService editingService;
    private final SpreadsheetLedgerSelectionService selectionService;

    @GetMapping
    public List<OperationExcelBookResponse> findActive() {
        return service.findActive();
    }

    @GetMapping("/{bookCode}/selection-options")
    public SpreadsheetLedgerSelectionResponse findSelectionOptions(
            @PathVariable String bookCode,
            @RequestParam String targetMonth
    ) {
        return selectionService.find(bookCode, targetMonth);
    }

    @PostMapping("/{bookCode}/generate")
    public SpreadsheetLedgerGenerateResponse generate(
            @PathVariable String bookCode,
            @Valid @RequestBody SpreadsheetLedgerGenerateRequest request
    ) {
        return service.generate(bookCode, request.targetMonth());
    }

    @PostMapping("/{bookCode}/generate-selected")
    public List<SpreadsheetLedgerGenerateResponse> generateSelected(
            @PathVariable String bookCode,
            @Valid @RequestBody SpreadsheetLedgerGenerateRequest request
    ) {
        return service.generateSelected(
                bookCode,
                request.targetMonth(),
                request.selectionValues()
        );
    }

    @PutMapping("/{bookCode}/generated/{targetMonth}")
    public SpreadsheetLedgerSaveResponse saveGenerated(
            @PathVariable String bookCode,
            @PathVariable String targetMonth,
            @Valid @RequestBody SpreadsheetLedgerSaveRequest request
    ) {
        return editingService.save(
                bookCode,
                targetMonth,
                request.workbook()
        );
    }

    @PutMapping(
            "/{bookCode}/generated/{targetMonth}/selections/{selectionValue}"
    )
    public SpreadsheetLedgerSaveResponse saveSelectedGenerated(
            @PathVariable String bookCode,
            @PathVariable String targetMonth,
            @PathVariable String selectionValue,
            @Valid @RequestBody SpreadsheetLedgerSaveRequest request
    ) {
        return editingService.save(
                bookCode,
                targetMonth,
                selectionValue,
                request.workbook()
        );
    }
}
