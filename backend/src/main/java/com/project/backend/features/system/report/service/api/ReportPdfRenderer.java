package com.project.backend.features.system.report.service.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportSignature;
import com.project.backend.features.system.report.repository.ReportSignatureRepository;
import com.project.backend.features.system.report.service.loader.ReportTemplateLoader;
import com.project.backend.features.system.report.util.ReportEngine;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportPdfRenderer {

    private static final String SIGNATURE_IMAGE_PARAM = "SIGNATURE_IMAGE";

    private final ReportTemplateLoader reportTemplateLoader;
    private final ReportSignatureRepository reportSignatureRepository;
    private final ReportEngine reportEngine;

    public byte[] render(
            ReportMaster reportMaster,
            Map<String, Object> params,
            List<Map<String, Object>> rows
    ) {
        try {
            String jrxmlContent = reportTemplateLoader.load(reportMaster.getTemplateFileName());

            Map<String, Object> renderParams = new java.util.LinkedHashMap<>();
            if (params != null) {
                renderParams.putAll(params);
            }

            if (Boolean.TRUE.equals(reportMaster.getUseSignature())) {
                applySignatureImageParam(reportMaster, renderParams);
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            JRMapCollectionDataSource dataSource =
                    new JRMapCollectionDataSource((Collection) rows);

            JasperPrint print = reportEngine.generate(jrxmlContent, renderParams, dataSource);

            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, pdfOut);
            return pdfOut.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF出力に失敗しました。", e);
        }
    }

    private void applySignatureImageParam(ReportMaster reportMaster, Map<String, Object> params) {
        ReportSignature signature = reportSignatureRepository
                .findFirstByReportMasterIdAndActiveFlagTrueAndDefaultFlagTrueOrderByDisplayOrderAscIdAsc(reportMaster.getId())
                .orElse(null);

        if (signature == null
                || signature.getSignatureImageData() == null
                || signature.getSignatureImageData().length == 0) {
            return;
        }

        params.put(SIGNATURE_IMAGE_PARAM, new ByteArrayInputStream(signature.getSignatureImageData()));
    }
}