package com.project.backend.features.system.rule.service.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.dto.*;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.exception.RuleConflictException;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.system.rule.service.RuleDataSourceCatalogService;
import com.project.backend.features.system.rule.service.RuleBeanCatalogService;
import com.project.backend.features.system.rule.entity.RuleDataSourceCatalog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleMasterValidator {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");
    private final RuleMasterRepository repository;
    private final RuleDataSourceCatalogService catalogService;
    private final RuleBeanCatalogService beanCatalogService;

    public void validateForCreate(RuleMasterSaveRequest request) {
        validate(request, null);
    }

    public void validateForUpdate(
            RuleMaster entity,
            RuleMasterSaveRequest request
    ) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "更新対象Ruleは必須です。"
            );
        }

        validate(request, entity.getId());

        if (!entity.getRuleName().equals(request.ruleName())) {
            throw new RuleConflictException(
                    "作成後のruleNameは変更できません。 current="
                            + entity.getRuleName()
                            + ", requested="
                            + request.ruleName()
            );
        }
    }

    private void validate(RuleMasterSaveRequest request, Long id) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        requireText(request.ruleName(), "ruleName");
        requireText(request.ruleDisplayName(), "ruleDisplayName");
        validateIdentifier(request.ruleName(), "ruleName");
        validateLength(request.ruleName(), "ruleName", 150);
        validateLength(
                request.ruleDisplayName(),
                "ruleDisplayName",
                200
        );

        if (request.ruleType() == null) {
            throw new IllegalArgumentException(
                    "ruleType は必須です。"
            );
        }

        RuleDslType dslType = request.dslType() != null
                ? request.dslType()
                : RuleDslType.MVEL;

        if (dslType == RuleDslType.JAVA_BEAN) {
            requireText(request.ruleBeanName(), "ruleBeanName");
            validateIdentifier(
                    request.ruleBeanName(),
                    "ruleBeanName"
            );
            beanCatalogService.requireRegistered(
                    request.ruleBeanName()
            );
        } else {
            requireText(request.dslText(), "dslText");
            RuleDslSafety.validate(request.dslText());
        }

        if (StringUtils.hasText(request.resultFactKey())) {
            validateIdentifier(
                    request.resultFactKey(),
                    "resultFactKey"
            );
        }

        if (request.priority() <= 0) {
            throw new IllegalArgumentException(
                    "priority は1以上で指定してください。"
            );
        }

        validateDuplicate(request, id);
        validateParameters(request.parameters());
        validateDataSources(request.dataSources());
    }

    private void validateDuplicate(RuleMasterSaveRequest request, Long id) {
        boolean exists = id == null
                ? repository.existsByRuleNameAndDeletedAtIsNull(request.ruleName())
                : repository.existsByRuleNameAndIdNotAndDeletedAtIsNull(request.ruleName(), id);

        if (exists) {
            throw new RuntimeException("ruleName が重複しています。 ruleName=" + request.ruleName());
        }
    }

    private void validateParameters(List<RuleParameterSaveRequest> parameters) {
        if (parameters == null) return;

        Set<String> names = new HashSet<>();

        for (RuleParameterSaveRequest parameter : parameters) {
            requireText(parameter.paramName(), "paramName");
            validateIdentifier(
                    parameter.paramName(),
                    "paramName"
            );

            if (!names.add(parameter.paramName())) {
                throw new RuntimeException("paramName が重複しています。 paramName=" + parameter.paramName());
            }

            if (parameter.dataType() == null) {
                throw new RuntimeException("parameter.dataType は必須です。 paramName=" + parameter.paramName());
            }

            if (parameter.orderNo() <= 0) {
                throw new RuntimeException("parameter.orderNo は1以上で指定してください。");
            }
        }
    }

    private void validateDataSources(List<RuleDataSourceSaveRequest> dataSources) {
        if (dataSources == null) return;

        Set<String> names = new HashSet<>();

        for (RuleDataSourceSaveRequest dataSource : dataSources) {
            requireText(dataSource.sourceName(), "sourceName");
            validateIdentifier(
                    dataSource.sourceName(),
                    "sourceName"
            );

            if (!names.add(dataSource.sourceName())) {
                throw new RuntimeException("sourceName が重複しています。 sourceName=" + dataSource.sourceName());
            }

            requireText(dataSource.catalogCode(), "catalogCode");
            validateColumns(dataSource.columns());
            validateCatalogDataSource(dataSource);

            if (dataSource.orderNo() <= 0) {
                throw new RuntimeException("dataSource.orderNo は1以上で指定してください。");
            }

        }
    }

    private void validateColumns(List<RuleColumnMappingSaveRequest> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ruleデータソースには1件以上の列Mappingが必要です。"
            );
        }

        Set<String> factKeys = new HashSet<>();

        for (RuleColumnMappingSaveRequest column : columns) {
            requireText(column.columnName(), "columnName");
            requireText(column.factKey(), "factKey");
            validateIdentifier(column.columnName(), "columnName");
            validateIdentifier(column.factKey(), "factKey");

            if (!factKeys.add(column.factKey())) {
                throw new RuntimeException("factKey が重複しています。 factKey=" + column.factKey());
            }

            if (column.dataType() == null) {
                throw new RuntimeException("column.dataType は必須です。 columnName=" + column.columnName());
            }

            if (column.orderNo() <= 0) {
                throw new RuntimeException("column.orderNo は1以上で指定してください。");
            }
        }
    }

    private void requireText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(label + " は必須です。");
        }
    }

    private void validateIdentifier(String value, String label) {
        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new RuntimeException(label + " に使用できない文字が含まれています。");
        }
    }

    private void validateLength(
            String value,
            String label,
            int maxLength
    ) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    label
                            + "は"
                            + maxLength
                            + "文字以内で指定してください。"
            );
        }
    }

    private void validateCatalogDataSource(
            RuleDataSourceSaveRequest dataSource
    ) {
        validateIdentifier(
                dataSource.catalogCode(),
                "catalogCode"
        );

        RuleDataSourceCatalog catalog =
                catalogService.findRequired(
                        dataSource.catalogCode()
                );

        var allowedColumns = catalog.getColumns().stream()
                .filter(column ->
                        column.getDeletedAt() == null
                                && column.isActiveFlag())
                .collect(java.util.stream.Collectors.toMap(
                        column -> column.getColumnName(),
                        column -> column
                ));

        for (RuleColumnMappingSaveRequest column :
                dataSource.columns()) {
            var catalogColumn =
                    allowedColumns.get(column.columnName());

            if (catalogColumn == null) {
                throw new IllegalArgumentException(
                        "カタログで許可されていないカラムです。 catalogCode="
                                + dataSource.catalogCode()
                                + ", columnName="
                                + column.columnName()
                );
            }

            if (catalogColumn.getDataType()
                    != column.dataType()) {
                throw new IllegalArgumentException(
                        "カタログとカラムのdataTypeが一致しません。 columnName="
                                + column.columnName()
                );
            }
        }
    }
}
