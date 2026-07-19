package com.project.backend.features.dailyreport.context;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.employee.entity.Employee;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyReportCalculationContext {

    Employee employee;

    DailyReport dailyReport;

    LocalDate targetDate;

    Map<String, Object> variables;

    public Map<String, Object> toParameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (employee != null) {
            parameters.put("employeeId", employee.getId());
        }

        if (dailyReport != null) {
            parameters.put("dailyReportId", dailyReport.getId());
        }

        if (targetDate != null) {
            parameters.put("targetDate", targetDate);
        }

        if (variables != null) {
            parameters.putAll(variables);
        }

        return parameters;
    }
}