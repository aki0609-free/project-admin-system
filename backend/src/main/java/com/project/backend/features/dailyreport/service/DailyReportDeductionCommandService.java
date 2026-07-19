package com.project.backend.features.dailyreport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReportDeduction;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportDeductionCommandService {

    private final DailyReportDeductionRepository repository;

    @SuppressWarnings("null")
    public void replaceAll(
            Long dailyReportId,
            List<DailyReportDeductionSaveRequest> requests
    ) {
        repository.deleteByDailyReportId(dailyReportId);

        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<DailyReportDeduction> entities = requests.stream()
                .map(request -> toEntity(dailyReportId, request))
                .toList();

        repository.saveAll(entities);
    }

    private DailyReportDeduction toEntity(
            Long dailyReportId,
            DailyReportDeductionSaveRequest request
    ) {
        DailyReportDeduction entity = new DailyReportDeduction();

        entity.setDailyReportId(dailyReportId);
        entity.setDeductionMasterId(request.deductionMasterId());
        entity.setDeductionCode(request.deductionCode());
        entity.setDeductionName(request.deductionName());
        entity.setAmount(request.amount() != null ? request.amount() : 0);

        return entity;
    }
}