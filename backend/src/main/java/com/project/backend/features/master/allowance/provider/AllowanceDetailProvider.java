package com.project.backend.features.master.allowance.provider;

import java.util.List;

import com.project.backend.features.master.allowance.dto.BaseAllowanceDetailResponse;
import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.enums.AllowanceDetailViewType;

public interface AllowanceDetailProvider {

    AllowanceDetailViewType supports();

    List<BaseAllowanceDetailResponse> getDetails(AllowanceMaster allowance);
}