package com.project.backend.features.dailyreport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportAllowanceSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReportAllowance;
import com.project.backend.features.dailyreport.repository.DailyReportAllowanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportAllowanceCommandService {

    private final DailyReportAllowanceRepository repository;

    @SuppressWarnings("null")
    public void replaceAll(
            Long dailyReportId,
            List<DailyReportAllowanceSaveRequest> requests
    ) {
        repository.deleteByDailyReportId(dailyReportId);

        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<DailyReportAllowance> entities = requests.stream()
                .map(request -> toEntity(dailyReportId, request))
                .toList();

        repository.saveAll(entities);
    }

    private DailyReportAllowance toEntity(
            Long dailyReportId,
            DailyReportAllowanceSaveRequest request
    ) {
        DailyReportAllowance entity = new DailyReportAllowance();

        entity.setDailyReportId(dailyReportId);
        entity.setAllowanceMasterId(request.allowanceMasterId());
        entity.setAllowanceCode(request.allowanceCode());
        entity.setAllowanceName(request.allowanceName());
        entity.setAmount(request.amount() != null ? request.amount() : 0);

        return entity;
    }
}