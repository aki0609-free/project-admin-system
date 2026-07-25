package com.project.backend.features.system.notice.service.validation;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public final class NoticeSqlSafety {

    private static final Pattern UNSAFE_WHERE_CLAUSE =
            Pattern.compile(
                    "(?i)(;|--|/\\*|\\*/|\\b(insert|update|delete|drop|alter|create|grant|revoke|truncate|call|execute|union)\\b)"
            );

    private NoticeSqlSafety() {
    }

    public static void validateWhereClause(
            String whereClause
    ) {
        if (!StringUtils.hasText(whereClause)) {
            return;
        }

        if (UNSAFE_WHERE_CLAUSE.matcher(whereClause).find()) {
            throw new IllegalArgumentException(
                    "whereClauseに使用できないSQL構文が含まれています。"
            );
        }
    }
}
