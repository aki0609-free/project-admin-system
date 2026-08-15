package com.project.backend.features.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.project.backend.features.application.dto.ApplicantUpdateRequest;
import com.project.backend.features.application.entity.Applicant;

class ApplicantMapperTest {

    private final ApplicantMapper mapper = Mappers.getMapper(ApplicantMapper.class);

    @Test
    void updateCanClearNullableFields() {
        Applicant applicant = Applicant.builder()
                .applicationNo("10")
                .name("応募者")
                .terminationDate(LocalDate.of(2026, 8, 1))
                .resignationReason("変更前")
                .build();

        ApplicantUpdateRequest request = new ApplicantUpdateRequest();
        request.setApplicationNo("10");
        request.setName("応募者");
        request.setTerminationDate(null);
        request.setResignationReason(null);

        mapper.update(applicant, request);

        assertThat(applicant.getTerminationDate()).isNull();
        assertThat(applicant.getResignationReason()).isNull();
    }
}
