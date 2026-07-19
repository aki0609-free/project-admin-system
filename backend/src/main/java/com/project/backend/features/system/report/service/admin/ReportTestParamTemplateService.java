package com.project.backend.features.system.report.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.dto.ReportTestParamTemplateResponse;
import com.project.backend.features.system.report.dto.ReportTestParamTemplateSaveRequest;
import com.project.backend.features.system.report.entity.ReportTestParamTemplate;
import com.project.backend.features.system.report.mapper.ReportTestParamTemplateMapper;
import com.project.backend.features.system.report.repository.ReportTestParamTemplateRepository;
import com.project.backend.features.system.report.service.validation.ReportTestParamTemplateValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportTestParamTemplateService {

    private final ReportTestParamTemplateRepository repository;
    private final ReportTestParamTemplateMapper mapper;
    private final ReportTestParamTemplateValidator validator;

    @Transactional(readOnly = true)
    public List<ReportTestParamTemplateResponse> findByReportCode(String reportCode) {
        return mapper.toResponseList(
                repository.findByReportCodeAndDeletedAtIsNullOrderByIdDesc(reportCode)
        );
    }

    @Transactional(readOnly = true)
    public ReportTestParamTemplateResponse findDefault(String reportCode) {
        return repository
                .findFirstByReportCodeAndDefaultFlagTrueAndDeletedAtIsNullOrderByIdDesc(reportCode)
                .map(mapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public ReportTestParamTemplateResponse create(ReportTestParamTemplateSaveRequest request) {
        validator.validateSaveRequest(request);

        ReportTestParamTemplate entity = new ReportTestParamTemplate();

        mapper.updateEntityFromRequest(request, entity);
        applyDefaults(entity);

        return mapper.toResponse(repository.save(entity));
    }

    private void applyDefaults(ReportTestParamTemplate entity) {
        if (entity.getDefaultFlag() == null) {
            entity.setDefaultFlag(false);
        }

        if (entity.getActiveFlag() == null) {
            entity.setActiveFlag(true);
        }
    }
}