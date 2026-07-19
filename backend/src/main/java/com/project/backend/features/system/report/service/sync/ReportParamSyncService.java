package com.project.backend.features.system.report.service.sync;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.dto.ReportParamRequest;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportParam;
import com.project.backend.features.system.report.enums.ReportParamControlType;
import com.project.backend.features.system.report.mapper.ReportMasterDtoMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportParamSyncService {

    private final ReportMasterDtoMapper mapper;

    public void sync(ReportMaster entity, List<ReportParamRequest> requests) {
        entity.clearParams();

        List<ReportParamRequest> safeRequests =
                requests != null ? requests : new ArrayList<>();

        for (ReportParamRequest request : safeRequests) {
            ReportParam param = new ReportParam();

            mapper.updateParamFromRequest(request, param);
            applyDefaults(param);

            entity.addParam(param);
        }
    }

    private void applyDefaults(ReportParam param) {
        if (param.getControlType() == null) {
            param.setControlType(ReportParamControlType.TEXT);
        }

        if (param.getRequiredFlag() == null) {
            param.setRequiredFlag(false);
        }

        if (param.getVisibleFlag() == null) {
            param.setVisibleFlag(true);
        }

        if (param.getMultipleFlag() == null) {
            param.setMultipleFlag(false);
        }

        if (param.getFilterFlag() == null) {
            param.setFilterFlag(true);
        }

        if (param.getDisplayOrder() == null) {
            param.setDisplayOrder(1);
        }

        if (param.getActiveFlag() == null) {
            param.setActiveFlag(true);
        }
    }
}