package com.project.backend.features.tax.dto;

import java.util.List;

public record ResidentTaxDraftSaveRequest(Integer fiscalYear, List<ResidentTaxEmployeeInput> employees) {}
