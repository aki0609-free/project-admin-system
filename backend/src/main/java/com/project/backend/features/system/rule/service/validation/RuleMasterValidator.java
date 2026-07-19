package com.project.backend.features.system.rule.service.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.dto.*;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleMasterValidator {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private final RuleMasterRepository repository;

    public void validateForCreate(RuleMasterSaveRequest request) {
        validate(request, null);
    }

    public void validateForUpdate(Long id, RuleMasterSaveRequest request) {
        validate(request, id);
    }

    private void validate(RuleMasterSaveRequest request, Long id) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        requireText(request.ruleName(), "ruleName");
        requireText(request.ruleDisplayName(), "ruleDisplayName");

        RuleDslType dslType = request.dslType() != null
                ? request.dslType()
                : RuleDslType.MVEL;

        if (dslType == RuleDslType.JAVA_BEAN) {
            requireText(request.ruleBeanName(), "ruleBeanName");
        } else {
            requireText(request.dslText(), "dslText");
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
            requireText(dataSource.tableName(), "tableName");

            if (!names.add(dataSource.sourceName())) {
                throw new RuntimeException("sourceName が重複しています。 sourceName=" + dataSource.sourceName());
            }

            validateIdentifier(dataSource.tableName(), "tableName");

            if (dataSource.orderNo() <= 0) {
                throw new RuntimeException("dataSource.orderNo は1以上で指定してください。");
            }

            validateColumns(dataSource.columns());
        }
    }

    private void validateColumns(List<RuleColumnMappingSaveRequest> columns) {
        if (columns == null) return;

        Set<String> factKeys = new HashSet<>();

        for (RuleColumnMappingSaveRequest column : columns) {
            requireText(column.columnName(), "columnName");
            requireText(column.factKey(), "factKey");
            validateIdentifier(column.columnName(), "columnName");

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
            throw new RuntimeException(label + " に使用できない文字が含まれています。 value=" + value);
        }
    }
}