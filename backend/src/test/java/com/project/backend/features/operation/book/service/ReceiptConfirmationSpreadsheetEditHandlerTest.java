package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.customer.dto.CustomerPaymentConfirmRequest;
import com.project.backend.features.customer.entity.CustomerTransaction;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;
import com.project.backend.features.customer.service.CustomerTransactionCommandService;

class ReceiptConfirmationSpreadsheetEditHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomerTransactionRepository repository =
            mock(CustomerTransactionRepository.class);
    private final CustomerTransactionCommandService commandService =
            mock(CustomerTransactionCommandService.class);
    private final ReceiptConfirmationSpreadsheetEditHandler handler =
            new ReceiptConfirmationSpreadsheetEditHandler(
                    repository,
                    commandService,
                    Clock.fixed(
                            Instant.parse("2026-03-31T01:00:00Z"),
                            ZoneId.of("Asia/Tokyo")
                    )
            );

    @Test
    void apply_shouldPersistOnlyEditableReceiptFields() throws Exception {
        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setId(1L);
        transaction.setCustomerId(10L);
        transaction.setTargetMonth("2026-02");
        transaction.setBillingAmount(1134014);
        when(repository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(transaction));

        var workbook = objectMapper.readTree(
                """
                {
                  "Workbook": {
                    "sheets": [{
                      "rows": [{
                        "cells": [
                          {"index":8,"value":1134014},
                          {"index":10,"value":1133464},
                          {"index":11,"value":550},
                          {"index":12,"value":0},
                          {"index":13,"value":0},
                          {"index":15,"value":"振込確認"},
                          {"index":16,"value":1},
                          {"index":17,"value":10}
                        ]
                      }]
                    }]
                  }
                }
                """
        );

        handler.apply("2026-02", workbook);

        ArgumentCaptor<CustomerPaymentConfirmRequest> request =
                ArgumentCaptor.forClass(CustomerPaymentConfirmRequest.class);
        verify(commandService).confirmPaymentFromLedger(
                eq(10L),
                eq(1L),
                eq("2026-02"),
                request.capture()
        );
        assertThat(request.getValue().confirmedPaymentDate())
                .isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(request.getValue().paidAmount()).isEqualTo(1133464);
        assertThat(request.getValue().fee()).isEqualTo(550);
        assertThat(request.getValue().offsetAmount()).isZero();
        assertThat(request.getValue().adjustmentAmount()).isZero();
        assertThat(request.getValue().note()).isEqualTo("振込確認");
    }

    @Test
    void apply_shouldRejectModifiedBillingAmount() throws Exception {
        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setId(1L);
        transaction.setCustomerId(10L);
        transaction.setTargetMonth("2026-02");
        transaction.setBillingAmount(1000);
        when(repository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(transaction));

        var workbook = objectMapper.readTree(
                """
                {
                  "Workbook": {
                    "sheets": [{
                      "rows": [{
                        "cells": [
                          {"index":8,"value":999},
                          {"index":10,"value":1000},
                          {"index":16,"value":1},
                          {"index":17,"value":10}
                        ]
                      }]
                    }]
                  }
                }
                """
        );

        assertThatThrownBy(() -> handler.apply("2026-02", workbook))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("請求金額は");
    }
}
