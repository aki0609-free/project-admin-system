package com.project.backend.features.dailyreport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportAllowanceResponse;
import com.project.backend.features.dailyreport.repository.DailyReportAllowanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportAllowanceQueryService {

    private final DailyReportAllowanceRepository repository;

    public List<DailyReportAllowanceResponse> findByDailyReportId(
            Long dailyReportId
    ) {
        return repository.findByDailyReportIdOrderByIdAsc(dailyReportId)
                .stream()
                .map(entity -> DailyReportAllowanceResponse.builder()
                        .id(entity.getId())
                        .allowanceMasterId(entity.getAllowanceMasterId())
                        .allowanceCode(entity.getAllowanceCode())
                        .allowanceName(entity.getAllowanceName())
                        .amount(entity.getAmount())
                        .build())
                .toList();
    }
}