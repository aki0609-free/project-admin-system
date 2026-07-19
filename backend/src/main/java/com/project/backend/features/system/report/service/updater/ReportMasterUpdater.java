package com.project.backend.features.system.report.service.updater;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.enums.ReportLayoutType;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.enums.ReportPreProcessType;
import com.project.backend.features.system.report.mapper.ReportMasterDtoMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportMasterUpdater {

    private final ReportMasterDtoMapper mapper;

    public void apply(ReportMaster entity, ReportMasterSaveRequest request) {
        mapper.updateMasterFromRequest(request, entity);
        applyDefaults(entity);
    }

    private void applyDefaults(ReportMaster entity) {
        if (entity.getPreProcessType() == null) {
            entity.setPreProcessType(ReportPreProcessType.NONE);
        }

        if (entity.getCleanupType() == null) {
            entity.setCleanupType(ReportCleanupType.NONE);
        }

        if (entity.getLayoutType() == null) {
            entity.setLayoutType(ReportLayoutType.SINGLE);
        }

        if (entity.getLayoutCount() == null) {
            entity.setLayoutCount(1);
        }

        if (entity.getOutputFormat() == null) {
            entity.setOutputFormat(ReportOutputFormat.PDF);
        }

        if (entity.getUseSignature() == null) {
            entity.setUseSignature(false);
        }

        if (entity.getPreviewEnabled() == null) {
            entity.setPreviewEnabled(true);
        }

        if (entity.getActiveFlag() == null) {
            entity.setActiveFlag(true);
        }
    }
}