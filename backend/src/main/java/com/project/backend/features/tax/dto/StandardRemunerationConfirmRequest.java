package com.project.backend.features.tax.dto;

public record StandardRemunerationConfirmRequest(
        String changeReason,
        boolean acknowledgeReclosing) {}
