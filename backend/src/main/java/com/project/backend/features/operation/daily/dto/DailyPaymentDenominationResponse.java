package com.project.backend.features.operation.daily.dto;

import lombok.Builder;

@Builder
public record DailyPaymentDenominationResponse(
        Integer yen10000,
        Integer yen5000,
        Integer yen1000,
        Integer yen500,
        Integer yen100,
        Integer yen50,
        Integer yen10,
        Integer yen5,
        Integer yen1
) {
}