package com.project.backend.features.system.excelbook.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterRequest;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.mapper.ExcelBookMasterMapper;
import com.project.backend.features.system.excelbook.enums.ExcelBookLayoutType;
import com.project.backend.features.system.excelbook.enums.ExcelBookSelectionMode;
import com.project.backend.features.system.excelbook.repository.ExcelBookMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExcelBookMasterCommandService {

    private static final Pattern BOOK_CODE_PATTERN =
            Pattern.compile("[A-Z0-9][A-Z0-9_-]{0,99}");

    private final ExcelBookMasterRepository repository;
    private final ExcelBookMasterMapper mapper;
    private final ExcelBookDataSourceCatalogService catalogService;
    private final ExcelBookTemplateRequirementResolver templateRequirementResolver;

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

        if (!entity.getBookCode().equals(request.bookCode())) {
            throw new IllegalArgumentException(
                    "bookCode は作成後に変更できません。"
            );
        }

        mapper.apply(entity, request);
        repository.save(entity);
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        ExcelBookMaster entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Excel台帳マスタが見つかりません。id=" + id));

        entity.setDeletedAt(Instant.now());
        entity.setActiveFlag(false);
        repository.save(entity);
    }

    private void validate(ExcelBookMasterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ExcelBookMasterRequest は必須です。");
        }
        if (request.bookCode() == null || request.bookCode().isBlank()) {
            throw new IllegalArgumentException("bookCode は必須です。");
        }
        if (!BOOK_CODE_PATTERN.matcher(request.bookCode()).matches()) {
            throw new IllegalArgumentException(
                    "bookCode は半角英大文字、数字、_、-で指定してください。"
            );
        }
        if (request.bookName() == null || request.bookName().isBlank()) {
            throw new IllegalArgumentException("bookName は必須です。");
        }
        if (request.dataSourceCode() == null
                || request.dataSourceCode().isBlank()) {
            throw new IllegalArgumentException(
                    "dataSourceCode は必須です。"
            );
        }
        if (request.rendererKey() != null
                && !request.rendererKey().matches(
                        "[A-Z0-9][A-Z0-9_-]{0,99}"
                )) {
            throw new IllegalArgumentException(
                    "rendererKey は半角英大文字、数字、_、-で指定してください。"
            );
        }
        String rendererKey = request.rendererKey() == null
                || request.rendererKey().isBlank()
                ? (request.layoutType() == null
                        ? ExcelBookLayoutType.REPEATING_ROW.name()
                        : request.layoutType().name())
                : request.rendererKey();
        if (templateRequirementResolver.requiresVariableMappings(rendererKey)
                && request.variableMappings().isEmpty()) {
            throw new IllegalArgumentException(
                    "このRendererにはテンプレート変数が1件以上必要です。"
            );
        }
        var catalog = catalogService.findRequired(
                request.dataSourceCode()
        );
        validateSelection(request);
        if (!Set.of("A3", "A4", "B5").contains(
                request.print().paperSize()
        )) {
            throw new IllegalArgumentException(
                    "印刷用紙はA3、A4、B5から指定してください。"
            );
        }
        Set<String> allowedColumns = catalog.getColumns().stream()
                .filter(column ->
                        column.isActiveFlag()
                                && column.getDeletedAt() == null
                )
                .map(column -> column.getColumnName())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> variableKeys = new HashSet<>();
        for (var mapping : request.variableMappings()) {
            if (mapping.variableKey() == null
                    || !mapping.variableKey().matches(
                            "[A-Za-z][A-Za-z0-9_.]{0,99}"
                    )) {
                throw new IllegalArgumentException(
                        "変数キーの形式が不正です: "
                                + mapping.variableKey()
                );
            }
            if (!variableKeys.add(mapping.variableKey())) {
                throw new IllegalArgumentException(
                        "変数キーが重複しています: "
                                + mapping.variableKey()
                );
            }
            if (!allowedColumns.contains(mapping.sourceColumn())) {
                throw new IllegalArgumentException(
                        "許可されていないデータソース項目です: "
                                + mapping.sourceColumn()
                );
            }
            if (!Set.of("CONTEXT", "ROW").contains(mapping.scope())) {
                throw new IllegalArgumentException(
                        "scope は CONTEXT または ROW で指定してください。"
                );
            }
            if (!Set.of(
                    "STRING",
                    "NUMBER",
                    "DATE",
                    "DATETIME",
                    "BOOLEAN"
            ).contains(mapping.dataType())) {
                throw new IllegalArgumentException(
                        "dataType が不正です: "
                                + mapping.dataType()
                );
            }
        }
    }

    private void validateSelection(ExcelBookMasterRequest request) {
        var selection = request.selection();
        if (selection.mode() == ExcelBookSelectionMode.NONE) {
            return;
        }
        if (selection.dataSourceCode() == null
                || selection.dataSourceCode().isBlank()) {
            throw new IllegalArgumentException(
                    "選択一覧のデータソースは必須です。"
            );
        }
        var selectionCatalog = catalogService.findRequired(
                selection.dataSourceCode()
        );
        Set<String> allowedColumns = selectionCatalog.getColumns().stream()
                .filter(column ->
                        column.isActiveFlag()
                                && column.getDeletedAt() == null
                )
                .map(column -> column.getColumnName())
                .collect(java.util.stream.Collectors.toSet());
        if (!allowedColumns.contains(selection.valueColumn())) {
            throw new IllegalArgumentException(
                    "選択値に許可されていない項目が指定されています: "
                            + selection.valueColumn()
            );
        }
        if (selection.displayColumns().isEmpty()) {
            throw new IllegalArgumentException(
                    "選択一覧の表示項目は1件以上必要です。"
            );
        }
        for (String displayColumn : selection.displayColumns()) {
            if (!allowedColumns.contains(displayColumn)) {
                throw new IllegalArgumentException(
                        "選択一覧に許可されていない表示項目があります: "
                                + displayColumn
                );
            }
        }
    }
}
