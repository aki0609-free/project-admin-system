package com.project.backend.features.master.allowance.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.project.backend.features.master.allowance.dto.AllowanceDetailResponse;
import com.project.backend.features.master.allowance.dto.AllowanceListItemResponse;
import com.project.backend.features.master.allowance.dto.AllowanceSaveRequest;
import com.project.backend.features.master.allowance.dto.BaseAllowanceDetailResponse;
import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.enums.AllowanceDetailViewType;

@Mapper(componentModel = "spring")
public interface AllowanceMapper {

    AllowanceMaster toEntity(AllowanceSaveRequest request);

    void update(
            @MappingTarget AllowanceMaster entity,
            AllowanceSaveRequest request
    );

    AllowanceListItemResponse toListItem(AllowanceMaster entity);

    default AllowanceDetailResponse toDetail(
            AllowanceMaster entity,
            Map<String, List<BaseAllowanceDetailResponse>> details
    ) {
        return new AllowanceDetailResponse(
                entity.getId(),
                entity.getAllowanceCode(),
                entity.getAllowanceName(),
                entity.getAllowanceType() == null ? null : entity.getAllowanceType().name(),
                entity.getCalculationType() == null ? null : entity.getCalculationType().name(),
                entity.getAllowanceUnit() == null ? null : entity.getAllowanceUnit().name(),
                entity.getDetailViewType() == null ? null : entity.getDetailViewType().name(),

                entity.getRuleName(),
                entity.getDefaultAmount(),
                Boolean.TRUE.equals(entity.getAllowManualInput()),
                entity.getMinAmount(),
                entity.getMaxAmount(),

                Boolean.TRUE.equals(entity.getTaxable()),
                Boolean.TRUE.equals(entity.getShowOnDailyStatement()),
                Boolean.TRUE.equals(entity.getShowOnMonthlyStatement()),
                entity.getDisplayOrder(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getNote(),

                details == null ? Collections.emptyMap() : details
        );
    }

    @AfterMapping
    default void applyDefaults(
            @MappingTarget AllowanceMaster entity
    ) {
        entity.setTaxable(Boolean.TRUE.equals(entity.getTaxable()));
        entity.setShowOnDailyStatement(Boolean.TRUE.equals(entity.getShowOnDailyStatement()));
        entity.setShowOnMonthlyStatement(Boolean.TRUE.equals(entity.getShowOnMonthlyStatement()));
        entity.setAllowManualInput(Boolean.TRUE.equals(entity.getAllowManualInput()));
        entity.setEnabled(entity.getEnabled() == null || Boolean.TRUE.equals(entity.getEnabled()));

        if (entity.getDetailViewType() == null) {
            entity.setDetailViewType(AllowanceDetailViewType.NONE);
        }
    }
}