package com.project.backend.features.dailyreport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportDeductionResponse;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportDeductionQueryService {

    private final DailyReportDeductionRepository repository;

    public List<DailyReportDeductionResponse> findByDailyReportId(
            Long dailyReportId
    ) {
        return repository.findByDailyReportIdOrderByIdAsc(dailyReportId)
                .stream()
                .map(entity -> DailyReportDeductionResponse.builder()
                        .id(entity.getId())
                        .deductionMasterId(entity.getDeductionMasterId())
                        .deductionCode(entity.getDeductionCode())
                        .deductionName(entity.getDeductionName())
                        .amount(entity.getAmount())
                        .build())
                .toList();
    }
}