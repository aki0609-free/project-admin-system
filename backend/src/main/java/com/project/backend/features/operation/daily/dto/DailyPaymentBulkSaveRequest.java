package com.project.backend.features.operation.daily.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPaymentBulkSaveRequest {

    @NotNull
    private LocalDate paymentDate;

    private List<DailyPaymentBulkSaveItemRequest> items = List.of();
}