package com.project.backend.features.system.rule.exception;

public class RuleConflictException extends RuntimeException {

    public RuleConflictException(String message) {
        super(message);
    }
}
