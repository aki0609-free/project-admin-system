package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.dto.ReportTestParamTemplateSaveRequest;

@Component
public class ReportTestParamTemplateValidator {

    public void validateSaveRequest(ReportTestParamTemplateSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.reportCode())) {
            throw new RuntimeException("reportCode は必須です。");
        }

        if (!StringUtils.hasText(request.templateName())) {
            throw new RuntimeException("templateName は必須です。");
        }

        if (!StringUtils.hasText(request.jsonText())) {
            throw new RuntimeException("jsonText は必須です。");
        }
    }
}