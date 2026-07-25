package com.project.backend.features.system.rule.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleReferenceChecker {

    private final AllowanceMasterRepository allowanceRepository;
    private final DeductionMasterRepository deductionRepository;

    public List<String> findReferenceTypes(String ruleName) {
        List<String> referenceTypes = new ArrayList<>();

        if (allowanceRepository
                .existsByRuleNameAndDeletedAtIsNull(ruleName)) {
            referenceTypes.add("手当マスタ");
        }

        if (deductionRepository
                .existsByRuleNameAndDeletedAtIsNull(ruleName)) {
            referenceTypes.add("控除マスタ");
        }

        return List.copyOf(referenceTypes);
    }

    public boolean isReferenced(String ruleName) {
        return !findReferenceTypes(ruleName).isEmpty();
    }
}
