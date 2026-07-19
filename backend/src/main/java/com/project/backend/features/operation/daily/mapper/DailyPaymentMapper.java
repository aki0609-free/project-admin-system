package com.project.backend.features.operation.daily.mapper;

import org.springframework.stereotype.Component;

import com.project.backend.features.operation.daily.dto.DailyPaymentResponse;
import com.project.backend.features.operation.daily.entity.DailyPayment;

@Component
public class DailyPaymentMapper {

    public DailyPaymentResponse toResponse(DailyPayment entity) {
        return DailyPaymentResponse.builder()
                .id(entity.getId())
                .paymentDate(entity.getPaymentDate())
                .employeeId(entity.getEmployeeId())
                .employeeCode(entity.getEmployeeCode())
                .employeeName(entity.getEmployeeName())
                .plannedAmount(entity.getPlannedAmount())
                .actualAmount(entity.getActualAmount())
                .status(entity.getStatus())
                .paidAt(entity.getPaidAt())
                .note(entity.getNote())
                .build();
    }
}