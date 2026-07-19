package com.project.backend.features.master.payrollitem.provider;

import java.util.List;

import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public interface PayrollItemValueProvider {

    PayrollItemTargetType supports();

    PayrollItemMasterSnapshot findMaster(
            PayrollItemValueRequest request
    );

    List<PayrollItemMasterSnapshot> findItems(
            PayrollItemQueryType queryType
    );
}