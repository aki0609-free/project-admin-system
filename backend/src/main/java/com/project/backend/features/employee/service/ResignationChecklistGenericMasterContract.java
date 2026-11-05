package com.project.backend.features.employee.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.masterdata.contract.GenericMasterBusinessContract;
import com.project.backend.features.system.masterdata.contract.GenericMasterRecordCandidate;

@Component
public class ResignationChecklistGenericMasterContract
        implements GenericMasterBusinessContract {

    public static final String MASTER_CODE = "RESIGNATION_CHECKLIST";

    @Override
    public String masterCode() {
        return MASTER_CODE;
    }

    @Override
    public void validate(List<GenericMasterRecordCandidate> records) {
        Set<String> codes = new HashSet<>();
        for (GenericMasterRecordCandidate record : records) {
            if (!record.activeFlag()) {
                continue;
            }
            String code = required(record, "CODE").trim().toUpperCase(Locale.ROOT);
            if (!codes.add(code)) {
                throw new IllegalArgumentException("同じTODOコードが既に登録されています。");
            }
            BigDecimal displayOrder = new BigDecimal(required(record, "DISPLAY_ORDER"));
            if (displayOrder.signum() < 0 || displayOrder.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("表示順は0以上の整数で指定してください。");
            }
        }
    }

    private String required(GenericMasterRecordCandidate record, String key) {
        Object value = record.values().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + "は必須です。");
        }
        return String.valueOf(value);
    }
}
