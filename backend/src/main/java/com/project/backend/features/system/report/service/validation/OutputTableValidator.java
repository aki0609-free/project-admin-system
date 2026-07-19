package com.project.backend.features.system.report.service.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OutputTableValidator {

    private static final Pattern SAFE_TABLE_NAME =
            Pattern.compile("^[A-Za-z0-9_]+$");

    public void validateTableName(
            String tableName
    ) {

        if (!StringUtils.hasText(tableName)) {
            throw new RuntimeException(
                    "テーブル名は必須です。"
            );
        }

        if (!SAFE_TABLE_NAME
                .matcher(tableName)
                .matches()) {

            throw new RuntimeException(
                    "不正なテーブル名です: "
                            + tableName
            );
        }
    }
}