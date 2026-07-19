package com.project.backend.features.system.imports.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.imports.entity.ImportTarget;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTargetLookupService {

    private final ImportTargetRepository repository;

    public ImportTarget find(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Import定義が見つかりません。 id=" + id));
    }

    public ImportTarget findActiveByTargetCode(String targetCode) {
        return repository.findByTargetCodeAndActiveFlagTrueAndDeletedAtIsNull(targetCode)
                .orElseThrow(() -> new RuntimeException("Import定義が見つかりません。 targetCode=" + targetCode));
    }
}