package com.project.backend.features.master.deduction.provider;

import java.util.List;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;

public interface DeductionDetailProvider {

    DeductionDetailViewType supports();

    List<BaseDeductionDetailResponse> getDetails(DeductionMaster deduction);
}