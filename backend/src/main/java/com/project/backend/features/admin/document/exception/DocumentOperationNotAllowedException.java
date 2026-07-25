package com.project.backend.features.admin.document.exception;

public class DocumentOperationNotAllowedException
        extends RuntimeException {

    public DocumentOperationNotAllowedException(String message) {
        super(message);
    }
}
