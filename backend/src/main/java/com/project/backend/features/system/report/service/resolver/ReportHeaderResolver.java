package com.project.backend.features.system.report.service.resolver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ReportHeaderResolver {

    public List<String> resolve(
            List<Map<String,Object>> rows
    ) {

        LinkedHashSet<String> headers =
                new LinkedHashSet<>();

        rows.forEach(row ->
                headers.addAll(row.keySet()));

        return new ArrayList<>(headers);
    }
}