package com.project.backend.features.application.validator;

import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ApplicantCodeValidator {

    private static final Set<String> CONTRACT_TYPES = Set.of(
            "PERMANENT", "CONTRACT", "TEMPORARY", "UNKNOWN"
    );

    private static final Set<String> GENDERS = Set.of(
            "MALE", "FEMALE", "UNKNOWN"
    );

    private static final Set<String> RECRUITMENT_STATUSES = Set.of(
            "JUST_CONTACT", "WITHDRAW", "BACKOUT", "INTERVIEW", "HIRED", "UNKNOWN"
    );

    private static final Set<String> ROOM_TYPES = Set.of(
            "ONE", "SHARED", "UNKNOWN"
    );

    private static final Set<String> YES_NO = Set.of(
            "YES", "NO"
    );

    public void validateApplicantCodes(
            String contractType,
            String gender,
            String recruitmentStatus,
            String needsDormitory,
            String roomType,
            String insuredBefore,
            String dormitoryExperience
    ) {
        validateCode("contractType", contractType, CONTRACT_TYPES);
        validateCode("gender", gender, GENDERS);
        validateCode("recruitmentStatus", recruitmentStatus, RECRUITMENT_STATUSES);
        validateCode("needsDormitory", needsDormitory, YES_NO);
        validateCode("roomType", roomType, ROOM_TYPES);
        validateCode("insuredBefore", insuredBefore, YES_NO);
        validateCode("dormitoryExperience", dormitoryExperience, YES_NO);
    }

    private void validateCode(String fieldName, String value, Set<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!allowedValues.contains(value)) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
    }
}