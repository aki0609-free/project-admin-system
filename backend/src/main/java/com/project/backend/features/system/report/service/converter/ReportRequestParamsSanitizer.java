package com.project.backend.features.system.report.service.converter;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ReportRequestParamsSanitizer {

    public Map<String, Object> sanitize(Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (params == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();

            if (value == null) {
                result.put(entry.getKey(), null);
                continue;
            }

            if (value instanceof byte[]
                    || value instanceof InputStream
                    || value instanceof Map<?, ?>
                    || value instanceof Iterable<?>) {
                result.put(entry.getKey(), String.valueOf(value));
                continue;
            }

            result.put(entry.getKey(), value);
        }

        return result;
    }
}