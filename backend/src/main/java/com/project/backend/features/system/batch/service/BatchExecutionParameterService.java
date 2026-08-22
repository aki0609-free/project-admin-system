package com.project.backend.features.system.batch.service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchExecutionParameterService {

    private static final int MAX_PARAMETERS = 50;
    private static final int MAX_COLLECTION_VALUES = 1000;
    private static final int MAX_STRING_LENGTH = 1000;
    private static final Pattern KEY_PATTERN =
            Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,99}");

    private final ObjectMapper objectMapper;

    public Map<String, Object> validateAndNormalize(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        if (params.size() > MAX_PARAMETERS) {
            throw new RuntimeException("バッチパラメータは50件以内で指定してください。");
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key == null || !KEY_PATTERN.matcher(key).matches()) {
                throw new RuntimeException("バッチパラメータ名が不正です。 key=" + key);
            }
            normalized.put(key, normalizeValue(key, value));
        });
        return Collections.unmodifiableMap(normalized);
    }

    private Object normalizeValue(String key, Object value) {
        if (value instanceof Map<?, ?>) {
            throw new RuntimeException("バッチパラメータにMapは指定できません。 key=" + key);
        }
        if (value instanceof Iterable<?> values) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : values) {
                addCollectionValue(key, normalized, item);
            }
            return Collections.unmodifiableList(normalized);
        }
        if (value != null && value.getClass().isArray()) {
            List<Object> normalized = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                addCollectionValue(key, normalized, Array.get(value, index));
            }
            return Collections.unmodifiableList(normalized);
        }
        validateScalar(key, value, false);
        return value;
    }

    private void addCollectionValue(
            String key,
            List<Object> normalized,
            Object value
    ) {
        if (normalized.size() >= MAX_COLLECTION_VALUES) {
            throw new RuntimeException("複数選択パラメータは1000件以内です。 key=" + key);
        }
        validateScalar(key, value, true);
        normalized.add(value);
    }

    private void validateScalar(
            String key,
            Object value,
            boolean collectionValue
    ) {
        if (collectionValue && value == null) {
            throw new RuntimeException("複数選択パラメータにnullは指定できません。 key=" + key);
        }
        if (value instanceof String text && text.length() > MAX_STRING_LENGTH) {
            throw new RuntimeException("バッチパラメータの文字列は1000文字以内です。 key=" + key);
        }
        if (value != null
                && !(value instanceof String)
                && !(value instanceof Number)
                && !(value instanceof Boolean)) {
            throw new RuntimeException("バッチパラメータの型が不正です。 key=" + key);
        }
    }

    public String serialize(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params == null ? Map.of() : params);
        } catch (Exception e) {
            throw new RuntimeException("バッチパラメータの保存に失敗しました。", e);
        }
    }

    public Map<String, Object> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return validateAndNormalize(objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {
                    }
            ));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("バッチパラメータの復元に失敗しました。", e);
        }
    }
}
