package com.project.backend.features.system.excelbook.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterResponse;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.mapper.ExcelBookMasterMapper;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelBookMasterQueryService {

    private final ExcelBookMasterRepository repository;
    private final ExcelBookMasterMapper mapper;
    private final ExcelBookTemplateRequirementResolver templateRequirementResolver;

    public List<ExcelBookMasterResponse> findAll() {
        return repository.findByDeletedAtIsNullOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ExcelBookMasterResponse findById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Excel台帳マスタが見つかりません。id=" + id));
    }

    private ExcelBookMasterResponse toResponse(
            ExcelBookMaster entity
    ) {
        String rendererKey = entity.getRendererKey() == null
                ? entity.getLayoutType().name()
                : entity.getRendererKey();
        return mapper.toResponse(
                entity,
                templateRequirementResolver.requiresTemplate(rendererKey)
        );
    }
}
