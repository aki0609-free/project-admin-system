package com.project.backend.features.system.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class BatchExecutionParameterServiceTest {

    private BatchExecutionParameterService service;

    @BeforeEach
    void setUp() {
        service = new BatchExecutionParameterService(new ObjectMapper());
    }

    @Test
    void scalarParametersCanBeSerializedAndRestored() {
        Map<String, Object> params = Map.of(
                "employeeId", 10,
                "targetMonth", "2026-07",
                "confirmed", true
        );

        Map<String, Object> restored =
                service.deserialize(service.serialize(params));

        assertThat(restored)
                .containsEntry("employeeId", 10)
                .containsEntry("targetMonth", "2026-07")
                .containsEntry("confirmed", true);
    }

    @Test
    void nestedParametersAreRejected() {
        assertThatThrownBy(() ->
                service.validateAndNormalize(
                        Map.of("employees", List.of(1, 2))
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("単一値");
    }

    @Test
    void unsafeParameterNameIsRejected() {
        assertThatThrownBy(() ->
                service.validateAndNormalize(
                        Map.of("employee id", 1)
                ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("パラメータ名");
    }
}
