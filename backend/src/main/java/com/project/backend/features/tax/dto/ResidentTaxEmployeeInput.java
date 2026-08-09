package com.project.backend.features.tax.dto;

import java.util.List;

public record ResidentTaxEmployeeInput(Long employeeId, List<ResidentTaxMonthInput> months) {}
