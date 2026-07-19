package com.project.backend.features.system.report.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

@Component
public class ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(ReportEngine.class);

    public JasperPrint generate(
            String jrxml,
            Map<String, Object> params,
            JRDataSource dataSource
    ) throws JRException {

        try {
            JasperReport report = JasperCompileManager.compileReport(
                    new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8))
            );

            return JasperFillManager.fillReport(report, params, dataSource);

        } catch (JRException e) {
            log.error("=== Jasper compile/fill failed ===");
            log.error("JRXML first 1000 chars:\n{}",
                    jrxml == null ? "null" : jrxml.substring(0, Math.min(jrxml.length(), 1000)));
            log.error("Params summary: {}", summarizeParams(params), e);
            throw e;
        }
    }

    private Map<String, Object> summarizeParams(Map<String, Object> params) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (params == null) {
            return summary;
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();

            if (value == null) {
                summary.put(entry.getKey(), null);
            } else if (value instanceof byte[]) {
                summary.put(entry.getKey(), "[byte[] length=" + ((byte[]) value).length + "]");
            } else if (value instanceof java.io.InputStream) {
                summary.put(entry.getKey(), "[" + value.getClass().getSimpleName() + "]");
            } else if (value instanceof JasperReport || value instanceof JasperPrint || value instanceof JRDataSource) {
                summary.put(entry.getKey(), "[" + value.getClass().getSimpleName() + "]");
            } else {
                summary.put(entry.getKey(), String.valueOf(value));
            }
        }

        return summary;
    }
}