package com.project.backend.features.system.report.service.api.exporter;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.report.entity.ReportMaster;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportExporterResolver {

    private final List<ReportExporter> exporters;

    public ReportExporter resolve(
            ReportMaster reportMaster
    ) {
        return exporters.stream()
                .filter(it -> it.supports(reportMaster))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Exporter未対応: "
                                + reportMaster.getOutputFormat()
                        ));
    }
}