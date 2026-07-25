package com.project.backend.features.system.report.service.builder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportMaster;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportFileNameBuilder {

    private final Clock clock;

    public String build(ReportMaster reportMaster, String extension) {
        String baseName = StringUtils.hasText(reportMaster.getFileName())
                ? reportMaster.getFileName()
                : reportMaster.getReportCode();

        String timestamp = LocalDateTime.now(clock)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return baseName + "_" + timestamp + "." + extension;
    }
}
