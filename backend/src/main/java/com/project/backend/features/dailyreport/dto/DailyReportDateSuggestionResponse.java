package com.project.backend.features.dailyreport.dto;

import java.time.LocalDate;

public record DailyReportDateSuggestionResponse(LocalDate workDate, LocalDate paymentDate) {}
