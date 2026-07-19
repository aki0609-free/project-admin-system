package com.project.backend.features.system.excelbook.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterResponse;
import com.project.backend.features.system.excelbook.mapper.ExcelBookMasterMapper;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelBookMasterQueryService {

    private final ExcelBookMasterRepository repository;
    private final ExcelBookMasterMapper mapper;

    public List<ExcelBookMasterResponse> findAll() {
        return repository.findByDeletedAtIsNullOrderByIdDesc()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ExcelBookMasterResponse findById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Excel台帳マスタが見つかりません。id=" + id));
    }
}