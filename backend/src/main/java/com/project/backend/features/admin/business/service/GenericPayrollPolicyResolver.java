package com.project.backend.features.admin.business.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.service.PayrollPolicySettingService.PayrollPolicyValues;
import com.project.backend.features.system.masterdata.entity.GenericMasterDefinition;
import com.project.backend.features.system.masterdata.entity.GenericMasterRecord;
import com.project.backend.features.system.masterdata.entity.GenericMasterStatus;
import com.project.backend.features.system.masterdata.repository.GenericMasterDefinitionRepository;
import com.project.backend.features.system.masterdata.repository.GenericMasterRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenericPayrollPolicyResolver {

    private static final TypeReference<Map<String, Object>> VALUE_MAP_TYPE =
            new TypeReference<>() { };

    private final GenericMasterDefinitionRepository definitionRepository;
    private final GenericMasterRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public Resolution resolve(LocalDate targetDate) {
        String tenantId = TenantContext.getTenantId();
        Optional<GenericMasterDefinition> definition = definitionRepository
                .findByTenantIdAndMasterCodeAndStatusAndActiveFlagTrueAndDeletedAtIsNull(
                        tenantId,
                        PayrollPolicyGenericMasterContract.MASTER_CODE,
                        GenericMasterStatus.PUBLISHED
                );
        if (definition.isEmpty()) {
            return Resolution.notConfigured();
        }
        List<PayrollPolicyValues> matches = recordRepository
                .findByTenantIdAndDefinitionIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        tenantId, definition.get().getId()
                )
                .stream()
                .map(this::readValues)
                .filter(values -> effectiveOn(values, targetDate))
                .map(this::toPolicyValues)
                .toList();
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "対象日に有効な汎用給与制度設定が重複しています。targetDate=" + targetDate
            );
        }
        return new Resolution(true, matches.stream().findFirst());
    }

    private boolean effectiveOn(Map<String, Object> values, LocalDate targetDate) {
        LocalDate from = LocalDate.parse(required(values, "EFFECTIVE_FROM"));
        String toText = text(values, "EFFECTIVE_TO");
        LocalDate to = toText == null ? null : LocalDate.parse(toText);
        return !targetDate.isBefore(from) && (to == null || !targetDate.isAfter(to));
    }

    private PayrollPolicyValues toPolicyValues(Map<String, Object> values) {
        return new PayrollPolicyValues(
                DayOfWeek.valueOf(required(values, "WEEK_START_DAY")),
                number(values, "WEEKLY_STATUTORY_HOURS"),
                number(values, "MONTHLY_OVERTIME_THRESHOLD_HOURS"),
                number(values, "OVERTIME_RATE"),
                number(values, "OVERTIME_OVER_THRESHOLD_RATE"),
                number(values, "NIGHT_PREMIUM_RATE"),
                number(values, "STATUTORY_HOLIDAY_RATE"),
                number(values, "DAILY_STANDARD_HOURS"),
                RoundingMode.valueOf(required(values, "AMOUNT_ROUNDING_MODE"))
        );
    }

    private Map<String, Object> readValues(GenericMasterRecord record) {
        try {
            return objectMapper.readValue(record.getValuesJson(), VALUE_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "汎用給与制度設定を読み込めません。id=" + record.getId(),
                    exception
            );
        }
    }

    private BigDecimal number(Map<String, Object> values, String key) {
        return new BigDecimal(required(values, key));
    }

    private String required(Map<String, Object> values, String key) {
        String value = text(values, key);
        if (value == null) {
            throw new IllegalStateException("汎用給与制度設定の必須項目がありません: " + key);
        }
        return value;
    }

    private String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null || String.valueOf(value).isBlank()
                ? null
                : String.valueOf(value);
    }

    public record Resolution(
            boolean configured,
            Optional<PayrollPolicyValues> values
    ) {
        static Resolution notConfigured() {
            return new Resolution(false, Optional.empty());
        }
    }
}
