package com.project.backend.features.system.rule.service.validation;

import java.util.regex.Pattern;

public final class RuleDslSafety {

    private static final Pattern UNSAFE_DSL =
            Pattern.compile(
                    "(?i)(\\b(import|new|class)\\b|getClass\\s*\\(|forName\\s*\\(|\\.class\\b|\\b(Runtime|System|ProcessBuilder|ClassLoader)\\b)"
            );

    private RuleDslSafety() {
    }

    public static void validate(String dslText) {
        if (dslText != null
                && UNSAFE_DSL.matcher(dslText).find()) {
            throw new IllegalArgumentException(
                    "DSLに使用できない構文が含まれています。"
            );
        }
    }
}
