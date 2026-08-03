package com.project.backend.features.system.report.service.api.exporter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.entity.ReportMaster;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExcelTemplateReportRendererRegistry {

    private final List<ExcelTemplateReportRenderer> renderers;

    public ExcelTemplateReportRenderer resolve(ReportMaster reportMaster) {
        return renderers.stream()
                .filter(renderer -> renderer.supports(reportMaster))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "ExcelテンプレートRendererが登録されていません。reportCode="
                                + reportMaster.getReportCode()
                ));
    }
}
