package com.project.backend.features.system.excelbook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterRequest;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.mapper.ExcelBookMasterMapper;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExcelBookMasterCommandService {

    private final ExcelBookMasterRepository repository;
    private final ExcelBookMasterMapper mapper;

    @SuppressWarnings("null")
    public Long create(ExcelBookMasterRequest request) {
        validate(request);

        if (repository.existsByBookCodeAndDeletedAtIsNull(request.bookCode())) {
            throw new IllegalArgumentException("bookCode が重複しています: " + request.bookCode());
        }

        return repository.save(mapper.toEntity(request)).getId();
    }

    @SuppressWarnings("null")
    public void update(Long id, ExcelBookMasterRequest request) {
        validate(request);

        ExcelBookMaster entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Excel台帳マスタが見つかりません。id=" + id));

        if (repository.existsByBookCodeAndIdNotAndDeletedAtIsNull(request.bookCode(), id)) {
            throw new IllegalArgumentException("bookCode が重複しています: " + request.bookCode());
        }

        mapper.apply(entity, request);
        repository.save(entity);
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        ExcelBookMaster entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Excel台帳マスタが見つかりません。id=" + id));

        repository.delete(entity);
    }

    private void validate(ExcelBookMasterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ExcelBookMasterRequest は必須です。");
        }
        if (request.bookCode() == null || request.bookCode().isBlank()) {
            throw new IllegalArgumentException("bookCode は必須です。");
        }
        if (request.bookName() == null || request.bookName().isBlank()) {
            throw new IllegalArgumentException("bookName は必須です。");
        }
        if (request.templateFilePath() == null || request.templateFilePath().isBlank()) {
            throw new IllegalArgumentException("templateFilePath は必須です。");
        }
        if (request.outputFilePath() == null || request.outputFilePath().isBlank()) {
            throw new IllegalArgumentException("outputFilePath は必須です。");
        }
        if (request.sourceName() == null || request.sourceName().isBlank()) {
            throw new IllegalArgumentException("sourceName は必須です。");
        }
    }
}