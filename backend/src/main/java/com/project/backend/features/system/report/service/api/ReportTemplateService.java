package com.project.backend.features.system.report.service.api;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.dto.ReportTemplateResponse;
import com.project.backend.features.system.report.service.builder.ReportTemplateKeyBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportTemplateService {

    private final StorageService storageService;
    private final ReportTemplateKeyBuilder keyBuilder;

    public List<ReportTemplateResponse> findAll() {
        return storageService.list(keyBuilder.templatePrefix())
                .stream()
                .filter(name -> name.endsWith(".jrxml"))
                .sorted()
                .map(name -> ReportTemplateResponse.builder()
                        .fileName(name)
                        .build())
                .toList();
    }
}