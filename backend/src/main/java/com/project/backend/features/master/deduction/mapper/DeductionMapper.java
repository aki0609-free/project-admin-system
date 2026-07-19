package com.project.backend.features.master.deduction.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.dto.DeductionDetailResponse;
import com.project.backend.features.master.deduction.dto.DeductionListItemResponse;
import com.project.backend.features.master.deduction.dto.DeductionSaveRequest;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;

@Component
public class DeductionMapper {

    public DeductionMaster toEntity(DeductionSaveRequest request) {
        DeductionMaster entity = new DeductionMaster();
        update(entity, request);
        return entity;
    }

    public void update(
            DeductionMaster entity,
            DeductionSaveRequest request
    ) {
        entity.setDeductionCode(request.deductionCode());
        entity.setDeductionName(request.deductionName());
        entity.setDeductionType(request.deductionType());
        entity.setCalculationType(request.calculationType());
        entity.setDeductionUnit(request.deductionUnit());
        entity.setDetailViewType(
                request.detailViewType() == null
                        ? DeductionDetailViewType.NONE
                        : request.detailViewType()
        );

        entity.setRuleName(request.ruleName());
        entity.setDefaultAmount(request.defaultAmount());
        entity.setAllowManualInput(Boolean.TRUE.equals(request.allowManualInput()));
        entity.setMinAmount(request.minAmount());
        entity.setMaxAmount(request.maxAmount());

        entity.setShowOnDailyStatement(Boolean.TRUE.equals(request.showOnDailyStatement()));
        entity.setShowOnMonthlyStatement(Boolean.TRUE.equals(request.showOnMonthlyStatement()));
        entity.setCarryToMonthlySettlement(Boolean.TRUE.equals(request.carryToMonthlySettlement()));
        entity.setDisplayOrder(request.displayOrder());
        entity.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
        entity.setNote(request.note());
    }

    public DeductionListItemResponse toListItem(
            DeductionMaster entity
    ) {
        return new DeductionListItemResponse(
                entity.getId(),
                entity.getDeductionCode(),
                entity.getDeductionName(),
                enumName(entity.getDeductionType()),
                enumName(entity.getCalculationType()),
                enumName(entity.getDeductionUnit()),
                enumName(entity.getDetailViewType()),

                entity.getRuleName(),
                entity.getDefaultAmount(),
                Boolean.TRUE.equals(entity.getAllowManualInput()),
                entity.getMinAmount(),
                entity.getMaxAmount(),

                Boolean.TRUE.equals(entity.getShowOnDailyStatement()),
                Boolean.TRUE.equals(entity.getShowOnMonthlyStatement()),
                Boolean.TRUE.equals(entity.getCarryToMonthlySettlement()),
                entity.getDisplayOrder(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getNote()
        );
    }

    public DeductionDetailResponse toDetail(
            DeductionMaster entity,
            Map<String, List<BaseDeductionDetailResponse>> details
    ) {
        DeductionListItemResponse base = toListItem(entity);

        return new DeductionDetailResponse(
                base.id(),
                base.deductionCode(),
                base.deductionName(),
                base.deductionType(),
                base.calculationType(),
                base.deductionUnit(),
                base.detailViewType(),

                base.ruleName(),
                base.defaultAmount(),
                base.allowManualInput(),
                base.minAmount(),
                base.maxAmount(),

                base.showOnDailyStatement(),
                base.showOnMonthlyStatement(),
                base.carryToMonthlySettlement(),
                base.displayOrder(),
                base.enabled(),
                base.note(),

                details == null ? Collections.emptyMap() : details
        );
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}