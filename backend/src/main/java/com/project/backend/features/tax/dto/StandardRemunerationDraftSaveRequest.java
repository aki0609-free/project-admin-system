package com.project.backend.features.tax.dto;

import java.util.List;

public record StandardRemunerationDraftSaveRequest(List<StandardRemunerationRowInput> rows) {}
