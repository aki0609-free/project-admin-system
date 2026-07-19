package com.project.backend.features.operation.monthly.mapper;

import org.springframework.stereotype.Component;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingResponse;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;

@Component
public class MonthlyClosingMapper {

    public MonthlyClosingResponse toResponse(MonthlyClosing entity) {

        if (entity == null) {
            return null;
        }

        return MonthlyClosingResponse.builder()
                .id(entity.getId())
                .targetMonth(entity.getTargetMonth())

                .status(entity.getStatus())

                .closingVersion(entity.getClosingVersion())

                .closingStartDate(entity.getClosingStartDate())
                .closingEndDate(entity.getClosingEndDate())

                .closingRuleType(entity.getClosingRuleType())
                .closingRuleValue(entity.getClosingRuleValue())

                .closedAt(entity.getClosedAt())

                .closedBy(entity.getClosedBy())

                .note(entity.getNote())
                .build();
    }
}